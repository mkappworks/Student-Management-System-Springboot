package com.mkappworks.enrollment.dto;

import com.mkappworks.enrollment.model.Enrollment;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private Long moduleId;
    private String moduleName;
    private String moduleCode;
    private Enrollment.EnrollmentStatus status;
    private String academicYear;
    private Integer semester;
    private LocalDate enrollmentDate;
    private LocalDateTime createdAt;
}
