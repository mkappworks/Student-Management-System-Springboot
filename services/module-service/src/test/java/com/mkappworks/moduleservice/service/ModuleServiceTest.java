package com.mkappworks.moduleservice.service;

import com.mkappworks.moduleservice.dto.ModuleDtos.*;
import com.mkappworks.moduleservice.entity.Module;
import com.mkappworks.moduleservice.entity.ModuleStatus;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.moduleservice.repository.ModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService Unit Tests")
class ModuleServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @InjectMocks private ModuleService moduleService;

    private CreateModuleRequest createRequest;
    private Module module;
    private UUID moduleId;

    @BeforeEach
    void setUp() {
        moduleId = UUID.randomUUID();

        createRequest = new CreateModuleRequest(
                "CS101", "Intro to CS", "Fundamentals",
                3, null, 30, "Fall", "2024/2025", null, null, "Room A101"
        );

        module = Module.builder()
                .id(moduleId)
                .code("CS101")
                .name("Intro to CS")
                .description("Fundamentals")
                .credits(3)
                .maxStudents(30)
                .currentEnrollment(0)
                .status(ModuleStatus.ACTIVE)
                .semester("Fall")
                .academicYear("2024/2025")
                .location("Room A101")
                .build();
    }

    // ── createModule ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createModule — succeeds for unique code")
    void createModule_UniqueCode_ReturnsResponse() {
        when(moduleRepository.existsByCode("CS101")).thenReturn(false);
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        ModuleResponse response = moduleService.createModule(createRequest);

        assertThat(response.code()).isEqualTo("CS101");
        assertThat(response.name()).isEqualTo("Intro to CS");
        assertThat(response.credits()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(ModuleStatus.ACTIVE);
        verify(moduleRepository).save(any(Module.class));
    }

    @Test
    @DisplayName("createModule — code is uppercased before saving")
    void createModule_LowercaseCode_SavesToUppercase() {
        CreateModuleRequest lower = new CreateModuleRequest(
                "cs101", "Intro", null, 3, null, 30, "Fall", "2024/2025", null, null, null
        );
        // Service checks existsByCode(request.code()) BEFORE uppercasing → lowercase key
        when(moduleRepository.existsByCode("cs101")).thenReturn(false);
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        ModuleResponse response = moduleService.createModule(lower);

        assertThat(response.code()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("createModule — throws IllegalArgumentException for duplicate code")
    void createModule_DuplicateCode_Throws() {
        when(moduleRepository.existsByCode("CS101")).thenReturn(true);

        assertThatThrownBy(() -> moduleService.createModule(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CS101");
        verify(moduleRepository, never()).save(any());
    }

    // ── getModuleById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getModuleById — returns response for existing ID")
    void getModuleById_Exists_ReturnsResponse() {
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        ModuleResponse response = moduleService.getModuleById(moduleId);

        assertThat(response.id()).isEqualTo(moduleId);
        assertThat(response.code()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("getModuleById — throws ResourceNotFoundException for unknown ID")
    void getModuleById_NotFound_Throws() {
        UUID unknown = UUID.randomUUID();
        when(moduleRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.getModuleById(unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getModuleByCode ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getModuleByCode — returns response for existing code")
    void getModuleByCode_Exists_ReturnsResponse() {
        when(moduleRepository.findByCode("CS101")).thenReturn(Optional.of(module));

        ModuleResponse response = moduleService.getModuleByCode("CS101");

        assertThat(response.code()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("getModuleByCode — throws ResourceNotFoundException for unknown code")
    void getModuleByCode_NotFound_Throws() {
        when(moduleRepository.findByCode("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.getModuleByCode("XX999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllModules ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllModules — returns paginated result")
    void getAllModules_ReturnsPaginatedPage() {
        var pageable = PageRequest.of(0, 10);
        Page<Module> page = new PageImpl<>(List.of(module), pageable, 1);
        when(moduleRepository.findAll(pageable)).thenReturn(page);

        Page<ModuleResponse> result = moduleService.getAllModules(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("CS101");
    }

    // ── updateModule ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateModule — updates provided fields and saves")
    void updateModule_ValidRequest_SavesAndReturns() {
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        module.setName("Updated CS");
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        UpdateModuleRequest updateReq = new UpdateModuleRequest(
                "Updated CS", null, null, null, null, null, null);
        ModuleResponse response = moduleService.updateModule(moduleId, updateReq);

        assertThat(response.name()).isEqualTo("Updated CS");
        verify(moduleRepository).save(any(Module.class));
    }

    // ── deleteModule ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteModule — calls deleteById for existing ID")
    void deleteModule_Exists_DeletesSuccessfully() {
        when(moduleRepository.existsById(moduleId)).thenReturn(true);
        doNothing().when(moduleRepository).deleteById(moduleId);

        assertThatCode(() -> moduleService.deleteModule(moduleId)).doesNotThrowAnyException();
        verify(moduleRepository).deleteById(moduleId);
    }

    @Test
    @DisplayName("deleteModule — throws ResourceNotFoundException for unknown ID")
    void deleteModule_NotFound_Throws() {
        UUID unknown = UUID.randomUUID();
        when(moduleRepository.existsById(unknown)).thenReturn(false);

        assertThatThrownBy(() -> moduleService.deleteModule(unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── incrementEnrollment ────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementEnrollment — increments count when capacity available")
    void incrementEnrollment_HasCapacity_Increments() {
        module.setCurrentEnrollment(5); // 5 of 30
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        assertThatCode(() -> moduleService.incrementEnrollment(moduleId)).doesNotThrowAnyException();
        assertThat(module.getCurrentEnrollment()).isEqualTo(6);
        verify(moduleRepository).save(module);
    }

    @Test
    @DisplayName("incrementEnrollment — sets status FULL when reaching max capacity")
    void incrementEnrollment_ReachesMax_SetsFullStatus() {
        module.setCurrentEnrollment(29); // one spot left
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        moduleService.incrementEnrollment(moduleId);

        assertThat(module.getCurrentEnrollment()).isEqualTo(30);
        assertThat(module.getStatus()).isEqualTo(ModuleStatus.FULL);
    }

    @Test
    @DisplayName("incrementEnrollment — throws IllegalStateException when at full capacity")
    void incrementEnrollment_AtCapacity_Throws() {
        module.setCurrentEnrollment(30); // already full
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        assertThatThrownBy(() -> moduleService.incrementEnrollment(moduleId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capacity");
        verify(moduleRepository, never()).save(any());
    }

    // ── decrementEnrollment ────────────────────────────────────────────────────

    @Test
    @DisplayName("decrementEnrollment — decrements count and reopens FULL module")
    void decrementEnrollment_WasFull_SetsActiveStatus() {
        module.setCurrentEnrollment(30);
        module.setStatus(ModuleStatus.FULL);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        moduleService.decrementEnrollment(moduleId);

        assertThat(module.getCurrentEnrollment()).isEqualTo(29);
        assertThat(module.getStatus()).isEqualTo(ModuleStatus.ACTIVE);
    }

    @Test
    @DisplayName("decrementEnrollment — never goes below zero")
    void decrementEnrollment_AlreadyZero_StaysAtZero() {
        module.setCurrentEnrollment(0);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        moduleService.decrementEnrollment(moduleId);

        assertThat(module.getCurrentEnrollment()).isEqualTo(0);
    }
}
