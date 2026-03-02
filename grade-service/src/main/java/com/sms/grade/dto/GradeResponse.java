package com.sms.grade.dto;

import com.sms.grade.model.Grade;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradeResponse {
    private Long id;
    private Long studentId;
    private Long moduleId;
    private Long teacherId;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private String letterGrade;
    private Grade.AssessmentType assessmentType;
    private String remarks;
    private Integer semester;
    private String academicYear;
    private LocalDateTime createdAt;
}
