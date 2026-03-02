package com.mkappworks.moduleservice.service;

import com.mkappworks.moduleservice.dto.ModuleDtos.*;
import com.mkappworks.moduleservice.entity.Module;
import com.mkappworks.moduleservice.entity.ModuleStatus;
import com.mkappworks.moduleservice.exception.ResourceNotFoundException;
import com.mkappworks.moduleservice.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final ModuleRepository moduleRepository;

    @Transactional
    public ModuleResponse createModule(CreateModuleRequest request) {
        if (moduleRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Module code already exists: " + request.code());
        }
        Module module = Module.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .credits(request.credits())
                .teacherId(request.teacherId())
                .maxStudents(request.maxStudents())
                .semester(request.semester())
                .academicYear(request.academicYear())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .location(request.location())
                .build();
        module = moduleRepository.save(module);
        log.info("Created module: {} - {}", module.getCode(), module.getName());
        return mapToResponse(module);
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModuleById(UUID id) {
        return mapToResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModuleByCode(String code) {
        return mapToResponse(moduleRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with code: " + code)));
    }

    @Transactional(readOnly = true)
    public Page<ModuleResponse> getAllModules(Pageable pageable) {
        return moduleRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ModuleResponse> getModulesByStatus(ModuleStatus status, Pageable pageable) {
        return moduleRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Transactional
    public ModuleResponse updateModule(UUID id, UpdateModuleRequest request) {
        Module module = findById(id);
        if (request.name() != null) module.setName(request.name());
        if (request.description() != null) module.setDescription(request.description());
        if (request.credits() != null) module.setCredits(request.credits());
        if (request.teacherId() != null) module.setTeacherId(request.teacherId());
        if (request.maxStudents() != null) module.setMaxStudents(request.maxStudents());
        if (request.location() != null) module.setLocation(request.location());
        if (request.status() != null) module.setStatus(request.status());
        return mapToResponse(moduleRepository.save(module));
    }

    @Transactional
    public void deleteModule(UUID id) {
        if (!moduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Module not found with id: " + id);
        }
        moduleRepository.deleteById(id);
        log.info("Deleted module: {}", id);
    }

    @Transactional
    public void incrementEnrollment(UUID moduleId) {
        Module module = findById(moduleId);
        if (!module.hasCapacity()) throw new IllegalStateException("Module is at capacity: " + moduleId);
        module.setCurrentEnrollment(module.getCurrentEnrollment() + 1);
        if (module.getMaxStudents() != null && module.getCurrentEnrollment() >= module.getMaxStudents()) {
            module.setStatus(ModuleStatus.FULL);
        }
        moduleRepository.save(module);
    }

    @Transactional
    public void decrementEnrollment(UUID moduleId) {
        Module module = findById(moduleId);
        int count = Math.max(0, module.getCurrentEnrollment() - 1);
        module.setCurrentEnrollment(count);
        if (module.getStatus() == ModuleStatus.FULL) module.setStatus(ModuleStatus.ACTIVE);
        moduleRepository.save(module);
    }

    private Module findById(UUID id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + id));
    }

    private ModuleResponse mapToResponse(Module m) {
        return new ModuleResponse(m.getId(), m.getCode(), m.getName(), m.getDescription(),
                m.getCredits(), m.getTeacherId(), m.getMaxStudents(), m.getCurrentEnrollment(),
                m.getStatus(), m.getSemester(), m.getAcademicYear(), m.getStartDate(), m.getEndDate(),
                m.getLocation(), m.hasCapacity(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
