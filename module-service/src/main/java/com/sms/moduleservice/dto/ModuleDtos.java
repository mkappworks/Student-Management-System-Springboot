package com.sms.moduleservice.dto;

import com.sms.moduleservice.entity.ModuleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ModuleDtos {

    public record CreateModuleRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull Integer credits,
            UUID teacherId,
            Integer maxStudents,
            @NotBlank String semester,
            @NotBlank String academicYear,
            LocalDate startDate,
            LocalDate endDate,
            String location
    ) {}

    public record UpdateModuleRequest(
            String name,
            String description,
            Integer credits,
            UUID teacherId,
            Integer maxStudents,
            String location,
            ModuleStatus status
    ) {}

    public record ModuleResponse(
            UUID id,
            String code,
            String name,
            String description,
            Integer credits,
            UUID teacherId,
            Integer maxStudents,
            Integer currentEnrollment,
            ModuleStatus status,
            String semester,
            String academicYear,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            boolean hasCapacity,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
