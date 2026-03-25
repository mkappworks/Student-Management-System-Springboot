package com.mkappworks.enrollment.controller;

import com.mkappworks.common.dto.ApiResponse;
import com.mkappworks.enrollment.dto.EnrollmentRequest;
import com.mkappworks.enrollment.dto.EnrollmentResponse;
import com.mkappworks.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Enrollments", description = "Enroll and manage student module enrollments")
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrollmentService.enroll(request), "Enrolled successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getEnrollmentById(id), "Enrollment retrieved"));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getStudentEnrollments(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getStudentEnrollments(studentId), "Enrollments retrieved"));
    }

    @PatchMapping("/{id}/drop")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> drop(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.dropEnrollment(id, reason), "Enrollment dropped"));
    }
}
