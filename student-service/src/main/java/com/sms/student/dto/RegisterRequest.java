package com.sms.student.dto;

import com.sms.student.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private User.Role role;
    private String programme;
}
