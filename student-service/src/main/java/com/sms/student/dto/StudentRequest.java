package com.sms.student.dto;

import com.sms.student.model.Student;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String address;

    @NotBlank(message = "Programme is required")
    private String programme;

    private Integer yearOfStudy;
    private Student.Gender gender;
}
