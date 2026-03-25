package com.mkappworks.teacher.controller;

import com.mkappworks.common.dto.ApiResponse;
import com.mkappworks.teacher.dto.TeacherRequest;
import com.mkappworks.teacher.dto.TeacherResponse;
import com.mkappworks.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Teachers", description = "CRUD operations for teacher records")
@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> create(@Valid @RequestBody TeacherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(teacherService.createTeacher(req), "Teacher created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<TeacherResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.getTeacherById(id), "Teacher retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Page<TeacherResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.getAllTeachers(pageable), "Teachers retrieved"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Page<TeacherResponse>>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.searchTeachers(query, pageable), "Search results"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest req) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.updateTeacher(id, req), "Teacher updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Teacher deleted successfully"));
    }
}
