package com.sms.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the API Gateway.
 *
 * This is the single entry point for all inbound traffic. Its role is traffic gating:
 * it decides whether a request is allowed to reach any downstream service at all.
 *
 * - Uses WebFlux (ServerHttpSecurity) because the gateway is a reactive application.
 * - Permits /api/v1/auth/** so login and register requests pass through unauthenticated.
 * - All other exchanges require authentication, enforced by JwtAuthenticationFilter
 *   (a GlobalFilter) which validates the JWT and injects X-User-Id / X-User-Role headers
 *   into the forwarded request.
 * - Does NOT know about users or passwords — it only validates tokens.
 *
 * Contrast with auth-service SecurityConfig, which sets up the authentication mechanism
 * itself (UserDetailsService, password encoding, token issuance).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/v1/auth/**", "/actuator/**", "/eureka/**").permitAll()
                        .anyExchange().authenticated())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
