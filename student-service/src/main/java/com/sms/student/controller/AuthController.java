package com.sms.student.controller;

import com.sms.student.dto.ApiResponse;
import com.sms.student.dto.AuthRequest;
import com.sms.student.dto.AuthResponse;
import com.sms.student.dto.RegisterRequest;
import com.sms.student.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request), "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.authenticate(request), "Login successful"));
    }
}
