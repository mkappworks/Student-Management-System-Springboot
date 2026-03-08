package com.mkappworks.grade;

import com.mkappworks.grade.dto.GradeRequest;
import com.mkappworks.grade.dto.GradeResponse;
import com.mkappworks.grade.exception.ResourceNotFoundException;
import com.mkappworks.grade.model.Grade;
import com.mkappworks.grade.repository.GradeRepository;
import com.mkappworks.grade.service.GradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradeService Unit Tests")
class GradeServiceTest {

    @Mock private GradeRepository gradeRepository;
    @InjectMocks private GradeService gradeService;

    private GradeRequest request;

    @BeforeEach
    void setUp() {
        request = GradeRequest.builder()
                .studentId(1L).moduleId(1L).teacherId(1L)
                .score(new BigDecimal("85.0"))
                .maxScore(new BigDecimal("100.0"))
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .semester(1).academicYear("2024/2025")
                .build();
    }

    /** Build a saved Grade entity matching what gradeRepository.save() returns. */
    private Grade buildSavedGrade(BigDecimal score, BigDecimal maxScore, String letter) {
        return Grade.builder()
                .id(1L).studentId(1L).moduleId(1L).teacherId(1L)
                .score(score).maxScore(maxScore)
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .letterGrade(letter)
                .semester(1).academicYear("2024/2025")
                .build();
    }

    // ── assignGrade ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignGrade — 85/100 → letter grade A, percentage 85.0000")
    void assignGrade_85out100_GradeA() {
        Grade saved = buildSavedGrade(new BigDecimal("85.0"), new BigDecimal("100.0"), "A");
        when(gradeRepository.save(any(Grade.class))).thenReturn(saved);

        GradeResponse response = gradeService.assignGrade(request);

        assertThat(response.getLetterGrade()).isEqualTo("A");
        assertThat(response.getScore()).isEqualByComparingTo(new BigDecimal("85.0"));
        assertThat(response.getPercentage()).isEqualByComparingTo(new BigDecimal("85.0000"));
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    @DisplayName("assignGrade — 90/100 → letter grade A+")
    void assignGrade_90out100_GradeAPlus() {
        request.setScore(new BigDecimal("90.0"));
        Grade saved = buildSavedGrade(new BigDecimal("90.0"), new BigDecimal("100.0"), "A+");
        when(gradeRepository.save(any(Grade.class))).thenReturn(saved);

        GradeResponse response = gradeService.assignGrade(request);

        assertThat(response.getLetterGrade()).isEqualTo("A+");
    }

    @Test
    @DisplayName("assignGrade — 70/100 → letter grade B")
    void assignGrade_70out100_GradeB() {
        request.setScore(new BigDecimal("70.0"));
        Grade saved = buildSavedGrade(new BigDecimal("70.0"), new BigDecimal("100.0"), "B");
        when(gradeRepository.save(any(Grade.class))).thenReturn(saved);

        assertThat(gradeService.assignGrade(request).getLetterGrade()).isEqualTo("B");
    }

    @Test
    @DisplayName("assignGrade — 40/100 → letter grade F")
    void assignGrade_40out100_GradeF() {
        request.setScore(new BigDecimal("40.0"));
        Grade saved = buildSavedGrade(new BigDecimal("40.0"), new BigDecimal("100.0"), "F");
        when(gradeRepository.save(any(Grade.class))).thenReturn(saved);

        assertThat(gradeService.assignGrade(request).getLetterGrade()).isEqualTo("F");
    }

    @Test
    @DisplayName("assignGrade — 50/100 → boundary grade D")
    void assignGrade_50out100_GradeD() {
        request.setScore(new BigDecimal("50.0"));
        Grade saved = buildSavedGrade(new BigDecimal("50.0"), new BigDecimal("100.0"), "D");
        when(gradeRepository.save(any(Grade.class))).thenReturn(saved);

        assertThat(gradeService.assignGrade(request).getLetterGrade()).isEqualTo("D");
    }

    // ── getStudentGrades ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentGrades — returns all grades for a student")
    void getStudentGrades_ReturnsAll() {
        Grade g1 = buildSavedGrade(new BigDecimal("85.0"), new BigDecimal("100.0"), "A");
        Grade g2 = buildSavedGrade(new BigDecimal("60.0"), new BigDecimal("100.0"), "C");
        when(gradeRepository.findByStudentId(1L)).thenReturn(List.of(g1, g2));

        List<GradeResponse> grades = gradeService.getStudentGrades(1L);

        assertThat(grades).hasSize(2);
        assertThat(grades.get(0).getStudentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getStudentGrades — returns empty list for student with no grades")
    void getStudentGrades_NoGrades_ReturnsEmpty() {
        when(gradeRepository.findByStudentId(999L)).thenReturn(List.of());

        List<GradeResponse> grades = gradeService.getStudentGrades(999L);

        assertThat(grades).isEmpty();
    }

    // ── getGradeById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getGradeById — returns grade for existing ID")
    void getGradeById_Exists_ReturnsResponse() {
        Grade saved = buildSavedGrade(new BigDecimal("75.0"), new BigDecimal("100.0"), "B");
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(saved));

        GradeResponse response = gradeService.getGradeById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getLetterGrade()).isEqualTo("B");
    }

    @Test
    @DisplayName("getGradeById — throws ResourceNotFoundException for unknown ID")
    void getGradeById_NotFound_Throws() {
        when(gradeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.getGradeById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateGrade ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateGrade — recalculates letter grade with new score")
    void updateGrade_NewScore_RecalculatesGrade() {
        Grade existing = buildSavedGrade(new BigDecimal("50.0"), new BigDecimal("100.0"), "D");
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));

        request.setScore(new BigDecimal("92.0"));
        Grade updated = buildSavedGrade(new BigDecimal("92.0"), new BigDecimal("100.0"), "A+");
        when(gradeRepository.save(any(Grade.class))).thenReturn(updated);

        GradeResponse response = gradeService.updateGrade(1L, request);

        assertThat(response.getLetterGrade()).isEqualTo("A+");
        verify(gradeRepository).save(any(Grade.class));
    }

    // ── deleteGrade ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteGrade — calls deleteById for existing ID")
    void deleteGrade_Exists_DeletesSuccessfully() {
        when(gradeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(gradeRepository).deleteById(1L);

        assertThatCode(() -> gradeService.deleteGrade(1L)).doesNotThrowAnyException();
        verify(gradeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteGrade — throws ResourceNotFoundException for unknown ID")
    void deleteGrade_NotFound_Throws() {
        when(gradeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> gradeService.deleteGrade(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
