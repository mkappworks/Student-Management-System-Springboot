package com.mkappworks.teacher.dto;

import com.mkappworks.teacher.model.Teacher;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherResponse {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;
    private Teacher.TeacherStatus status;
    private Teacher.EmploymentType employmentType;
    private Set<Long> assignedModuleIds;
    private LocalDateTime createdAt;
}
