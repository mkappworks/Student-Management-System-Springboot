package com.mkappworks.authservice.dto;

import com.mkappworks.authservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotNull Role role,
            UUID referenceId
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String username,
            String role
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record UserResponse(
            UUID id,
            String username,
            String email,
            Role role,
            UUID referenceId,
            boolean enabled
    ) {}
}
