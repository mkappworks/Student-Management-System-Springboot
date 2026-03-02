package com.sms.grade.dto;

import com.sms.grade.model.Grade;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradeRequest {
    @NotNull private Long studentId;
    @NotNull private Long moduleId;
    @NotNull private Long teacherId;
    @NotNull @DecimalMin("0") private BigDecimal score;
    @NotNull @DecimalMin("1") private BigDecimal maxScore;
    @NotNull private Grade.AssessmentType assessmentType;
    private String remarks;
    private Integer semester;
    private String academicYear;
}
