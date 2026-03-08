package com.mkappworks.moduleservice.controller;

import com.mkappworks.common.dto.ApiResponse;
import com.mkappworks.moduleservice.dto.ModuleDtos.*;
import com.mkappworks.moduleservice.entity.ModuleStatus;
import com.mkappworks.moduleservice.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(@Valid @RequestBody CreateModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(moduleService.createModule(request), "Module created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ModuleResponse>> getModuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getModuleById(id), "Module retrieved"));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ModuleResponse>> getModuleByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getModuleByCode(code), "Module retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Page<ModuleResponse>>> getAllModules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getAllModules(PageRequest.of(page, size)), "Modules retrieved"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Page<ModuleResponse>>> getModulesByStatus(
            @PathVariable ModuleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getModulesByStatus(status, PageRequest.of(page, size)), "Modules retrieved"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID id,
            @RequestBody UpdateModuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.updateModule(id, request), "Module updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Module deleted successfully"));
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> incrementEnrollment(@PathVariable UUID id) {
        moduleService.incrementEnrollment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Enrollment incremented"));
    }

    @PostMapping("/{id}/unenroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> decrementEnrollment(@PathVariable UUID id) {
        moduleService.decrementEnrollment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Enrollment decremented"));
    }
}
