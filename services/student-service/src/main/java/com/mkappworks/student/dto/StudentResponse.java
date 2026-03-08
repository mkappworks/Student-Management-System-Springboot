package com.mkappworks.student.dto;

import com.mkappworks.student.model.Student;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentResponse {
    private Long id;
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String programme;
    private Integer yearOfStudy;
    private Double gpa;
    private Student.StudentStatus status;
    private Student.Gender gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
