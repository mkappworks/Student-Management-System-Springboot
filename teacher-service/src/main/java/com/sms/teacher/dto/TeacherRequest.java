package com.sms.teacher.dto;

import com.sms.teacher.model.Teacher;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    @NotBlank private String department;
    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;
    private Teacher.EmploymentType employmentType;
}
