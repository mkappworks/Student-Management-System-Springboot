package com.mkappworks.authservice.config;

import com.mkappworks.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Auth Service.
 *
 * This service owns the authentication mechanism itself — it validates credentials
 * and issues JWTs. This config wires together the beans that make that possible.
 *
 * - Uses standard servlet HttpSecurity (not WebFlux) — auth-service is a blocking app.
 * - Configures UserDetailsService to load users from the database via UserRepository.
 * - Sets up DaoAuthenticationProvider with BCrypt password encoding.
 * - Exposes AuthenticationManager so AuthService can call authenticate() on login.
 * - Session policy is STATELESS — this service issues JWTs, not sessions.
 *
 * Contrast with api-gateway SecurityConfig, which only gates traffic (validates tokens)
 * and has no knowledge of users or passwords.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    // All /api/v1/auth/** endpoints must be open here even though the gateway also permits them —
    // the gateway forwards the raw request, so auth-service must allow it through its own filter chain too.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // This service issues JWTs instead of sessions — no server-side session state needed.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .build();
    }

    // Bridges Spring Security's authentication mechanism to the UserRepository so credentials
    // can be looked up from the database during login.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        // BCrypt encoder must be set explicitly so the provider knows how to compare stored hashes.
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Exposed as a bean so AuthService can call authenticationManager.authenticate() directly on login.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
