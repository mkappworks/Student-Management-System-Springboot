package com.mkappworks.enrollment.service;

import com.mkappworks.enrollment.dto.EnrollmentRequest;
import com.mkappworks.enrollment.dto.EnrollmentResponse;
import com.mkappworks.enrollment.exception.DuplicateResourceException;
import com.mkappworks.enrollment.exception.ResourceNotFoundException;
import com.mkappworks.enrollment.model.Enrollment;
import com.mkappworks.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Unit Tests")
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @InjectMocks private EnrollmentService enrollmentService;

    private EnrollmentRequest request;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        request = EnrollmentRequest.builder()
                .studentId(1L).moduleId(10L)
                .academicYear("2024/2025").semester(1)
                .build();

        enrollment = Enrollment.builder()
                .id(1L).studentId(1L).moduleId(10L)
                .academicYear("2024/2025").semester(1)
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrollmentDate(LocalDate.now())
                .build();
    }

    // ── enroll ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("enroll — succeeds for new student-module combination")
    void enroll_NewCombination_ReturnsResponse() {
        when(enrollmentRepository.existsByStudentIdAndModuleIdAndAcademicYearAndSemester(
                1L, 10L, "2024/2025", 1)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        EnrollmentResponse response = enrollmentService.enroll(request);

        assertThat(response.getStudentId()).isEqualTo(1L);
        assertThat(response.getModuleId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.ENROLLED);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("enroll — throws DuplicateResourceException for already enrolled combination")
    void enroll_AlreadyEnrolled_Throws() {
        when(enrollmentRepository.existsByStudentIdAndModuleIdAndAcademicYearAndSemester(
                1L, 10L, "2024/2025", 1)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already enrolled");
        verify(enrollmentRepository, never()).save(any());
    }

    // ── getEnrollmentById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getEnrollmentById — returns response for existing ID")
    void getEnrollmentById_Exists_ReturnsResponse() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.getEnrollmentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAcademicYear()).isEqualTo("2024/2025");
    }

    @Test
    @DisplayName("getEnrollmentById — throws ResourceNotFoundException for unknown ID")
    void getEnrollmentById_NotFound_Throws() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getEnrollmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getStudentEnrollments ──────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentEnrollments — returns all enrollments for a student")
    void getStudentEnrollments_ReturnsAll() {
        Enrollment second = Enrollment.builder()
                .id(2L).studentId(1L).moduleId(20L)
                .academicYear("2024/2025").semester(2)
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .build();
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment, second));

        List<EnrollmentResponse> result = enrollmentService.getStudentEnrollments(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getStudentId().equals(1L));
    }

    @Test
    @DisplayName("getStudentEnrollments — returns empty list for student with no enrollments")
    void getStudentEnrollments_NoEnrollments_ReturnsEmpty() {
        when(enrollmentRepository.findByStudentId(999L)).thenReturn(List.of());

        List<EnrollmentResponse> result = enrollmentService.getStudentEnrollments(999L);

        assertThat(result).isEmpty();
    }

    // ── dropEnrollment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("dropEnrollment — sets status DROPPED with reason and date")
    void dropEnrollment_ValidId_SetsDroppedStatus() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollment.setDropReason("Personal reasons");
        enrollment.setDropDate(LocalDate.now());
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        EnrollmentResponse response = enrollmentService.dropEnrollment(1L, "Personal reasons");

        assertThat(response.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.DROPPED);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("dropEnrollment — throws ResourceNotFoundException for unknown ID")
    void dropEnrollment_NotFound_Throws() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.dropEnrollment(999L, "reason"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(enrollmentRepository, never()).save(any());
    }
}
