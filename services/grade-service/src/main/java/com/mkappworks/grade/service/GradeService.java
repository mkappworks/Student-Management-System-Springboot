package com.mkappworks.grade.service;

import com.mkappworks.grade.dto.GradeRequest;
import com.mkappworks.grade.dto.GradeResponse;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.grade.model.Grade;
import com.mkappworks.grade.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GradeService {

    private final GradeRepository gradeRepository;

    public GradeResponse assignGrade(GradeRequest request) {
        Grade grade = Grade.builder()
                .studentId(request.getStudentId())
                .moduleId(request.getModuleId())
                .teacherId(request.getTeacherId())
                .score(request.getScore())
                .maxScore(request.getMaxScore())
                .assessmentType(request.getAssessmentType())
                .remarks(request.getRemarks())
                .semester(request.getSemester())
                .academicYear(request.getAcademicYear())
                .letterGrade(calculateLetterGrade(request.getScore(), request.getMaxScore()))
                .build();
        return mapToResponse(gradeRepository.save(grade));
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> getStudentGrades(Long studentId) {
        return gradeRepository.findByStudentId(studentId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GradeResponse getGradeById(Long id) {
        return mapToResponse(gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", id)));
    }

    public GradeResponse updateGrade(Long id, GradeRequest request) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", id));
        grade.setScore(request.getScore());
        grade.setMaxScore(request.getMaxScore());
        grade.setRemarks(request.getRemarks());
        grade.setLetterGrade(calculateLetterGrade(request.getScore(), request.getMaxScore()));
        return mapToResponse(gradeRepository.save(grade));
    }

    public void deleteGrade(Long id) {
        if (!gradeRepository.existsById(id)) throw new ResourceNotFoundException("Grade", "id", id);
        gradeRepository.deleteById(id);
    }

    private String calculateLetterGrade(BigDecimal score, BigDecimal maxScore) {
        double pct = score.divide(maxScore, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        if (pct >= 50) return "D";
        return "F";
    }

    private GradeResponse mapToResponse(Grade g) {
        BigDecimal pct = g.getScore().divide(g.getMaxScore(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return GradeResponse.builder()
                .id(g.getId()).studentId(g.getStudentId()).moduleId(g.getModuleId())
                .teacherId(g.getTeacherId()).score(g.getScore()).maxScore(g.getMaxScore())
                .percentage(pct).letterGrade(g.getLetterGrade())
                .assessmentType(g.getAssessmentType()).remarks(g.getRemarks())
                .semester(g.getSemester()).academicYear(g.getAcademicYear())
                .createdAt(g.getCreatedAt()).build();
    }
}
