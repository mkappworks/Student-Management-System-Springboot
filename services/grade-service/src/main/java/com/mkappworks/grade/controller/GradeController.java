package com.mkappworks.grade.controller;

import com.mkappworks.common.dto.ApiResponse;
import com.mkappworks.grade.dto.GradeRequest;
import com.mkappworks.grade.dto.GradeResponse;
import com.mkappworks.grade.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Grades", description = "Manage student grades per module")
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<GradeResponse>> assignGrade(@Valid @RequestBody GradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gradeService.assignGrade(request), "Grade assigned successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GradeResponse>> getGrade(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.getGradeById(id), "Grade retrieved"));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<GradeResponse>>> getStudentGrades(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.getStudentGrades(studentId), "Grades retrieved"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<GradeResponse>> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.updateGrade(id, request), "Grade updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Grade deleted successfully"));
    }
}
