package com.sms.teacher.controller;

import com.sms.teacher.dto.TeacherRequest;
import com.sms.teacher.dto.TeacherResponse;
import com.sms.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @GetMapping
    public ResponseEntity<Page<TeacherResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(teacherService.getAllTeachers(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TeacherResponse>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(teacherService.searchTeachers(query, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<TeacherResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest req) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
