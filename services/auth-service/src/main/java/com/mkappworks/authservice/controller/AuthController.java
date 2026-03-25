package com.mkappworks.authservice.controller;

import com.mkappworks.authservice.dto.AuthDtos.*;
import com.mkappworks.authservice.service.AuthService;
import com.mkappworks.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setRefreshCookie(response, authResponse.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setRefreshCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "sms_refresh", required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest body,
            HttpServletResponse response) {
        // Cookie is the primary channel; body fallback preserves backward compatibility with older clients.
        String refreshToken = cookieToken != null ? cookieToken
                : (body != null ? body.refreshToken() : null);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Refresh token is required"));
        }
        AuthResponse authResponse = authService.refreshToken(new RefreshRequest(refreshToken));
        setRefreshCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    // Max-Age=0 is the standard mechanism to instruct the browser to delete the cookie immediately.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("sms_refresh", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth Service is running", "OK"));
    }

    // HttpOnly prevents JS access; SameSite=Lax blocks CSRF on cross-site navigations.
    // secure=false allows HTTP in local dev — set to true behind HTTPS in production.
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("sms_refresh", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
