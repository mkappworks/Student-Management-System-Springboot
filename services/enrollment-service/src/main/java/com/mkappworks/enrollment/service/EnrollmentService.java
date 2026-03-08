package com.mkappworks.enrollment.service;

import com.mkappworks.enrollment.dto.EnrollmentRequest;
import com.mkappworks.enrollment.dto.EnrollmentResponse;
import com.mkappworks.enrollment.exception.DuplicateResourceException;
import com.mkappworks.enrollment.exception.ResourceNotFoundException;
import com.mkappworks.enrollment.model.Enrollment;
import com.mkappworks.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentResponse enroll(EnrollmentRequest request) {
        if (enrollmentRepository.existsByStudentIdAndModuleIdAndAcademicYearAndSemester(
                request.getStudentId(), request.getModuleId(),
                request.getAcademicYear(), request.getSemester())) {
            throw new DuplicateResourceException("Student already enrolled in this module");
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .moduleId(request.getModuleId())
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrollmentDate(LocalDate.now())
                .build();

        return mapToResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long id) {
        return mapToResponse(enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id)));
    }

    public EnrollmentResponse dropEnrollment(Long id, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollment.setDropDate(LocalDate.now());
        enrollment.setDropReason(reason);
        return mapToResponse(enrollmentRepository.save(enrollment));
    }

    private EnrollmentResponse mapToResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId()).studentId(e.getStudentId()).moduleId(e.getModuleId())
                .status(e.getStatus()).academicYear(e.getAcademicYear())
                .semester(e.getSemester()).enrollmentDate(e.getEnrollmentDate())
                .createdAt(e.getCreatedAt()).build();
    }
}
