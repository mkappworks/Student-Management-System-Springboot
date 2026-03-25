package com.mkappworks.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.authservice.config.SecurityConfig;
import com.mkappworks.authservice.dto.AuthDtos.*;
import com.mkappworks.authservice.entity.Role;
import com.mkappworks.authservice.repository.UserRepository;
import com.mkappworks.authservice.service.AuthService;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer (MockMvc) tests for AuthController.
 *
 * auth-service uses DaoAuthenticationProvider (no JwtAuthenticationFilter in the chain),
 * so unlike student-service tests we don't need to mock/passthrough a JWT filter.
 * All /api/v1/auth/** endpoints are permitAll(), so no @WithMockUser is required.
 *
 * MockBeans required:
 *   AuthService     — the service being delegated to
 *   UserRepository  — SecurityConfig.userDetailsService() calls userRepository.findByUsername()
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private UserRepository userRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REFRESH_URL  = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL   = "/api/v1/auth/logout";

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse("access.token.here", "refresh.token.here",
                "Bearer", 3600L, "testuser", "STUDENT");
    }

    // ── REGISTER ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register — valid request → 201 with Set-Cookie")
    void register_ValidRequest_Returns201WithCookie() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        var request = new RegisterRequest("testuser", "test@example.com", "Password@123",
                Role.STUDENT, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("sms_refresh=")));
    }

    @Test
    @DisplayName("POST /register — service throws IllegalArgumentException → 400")
    void register_ExistingUsername_Returns400() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        var request = new RegisterRequest("duplicate", "dup@example.com", "Password@123",
                Role.STUDENT, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login — valid credentials → 200 with Set-Cookie")
    void login_ValidCredentials_Returns200WithCookie() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse());

        var request = new LoginRequest("testuser", "Password@123");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("sms_refresh=")));
    }

    @Test
    @DisplayName("POST /login — bad credentials → 400")
    void login_InvalidCredentials_Returns400() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Bad credentials"));

        var request = new LoginRequest("testuser", "WrongPass");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── REFRESH ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /refresh — valid sms_refresh cookie → 200 with new Set-Cookie")
    void refresh_WithCookieToken_Returns200() throws Exception {
        when(authService.refreshToken(any(RefreshRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post(REFRESH_URL)
                        .cookie(new jakarta.servlet.http.Cookie("sms_refresh", "valid.refresh.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("POST /refresh — no cookie and no body → 400")
    void refresh_WithNoToken_Returns400() throws Exception {
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── LOGOUT ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /logout → 200 with Max-Age=0 cookie clearing")
    void logout_Returns200AndClearsCookie() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }
}
