package com.sms.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentRequest {
    @NotNull private Long studentId;
    @NotNull private Long moduleId;
    private String academicYear;
    private Integer semester;
}
