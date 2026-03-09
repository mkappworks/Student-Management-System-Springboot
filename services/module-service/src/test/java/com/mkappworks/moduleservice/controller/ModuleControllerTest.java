package com.mkappworks.moduleservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.common.security.JwtAuthFilter;
import com.mkappworks.moduleservice.config.SecurityConfig;
import com.mkappworks.moduleservice.dto.ModuleDtos.CreateModuleRequest;
import com.mkappworks.moduleservice.dto.ModuleDtos.ModuleResponse;
import com.mkappworks.moduleservice.dto.ModuleDtos.UpdateModuleRequest;
import com.mkappworks.moduleservice.entity.ModuleStatus;
import com.mkappworks.moduleservice.service.ModuleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ModuleController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ModuleController Web Layer Tests")
class ModuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ModuleService moduleService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private UUID moduleId;
    private CreateModuleRequest validCreateRequest;
    private UpdateModuleRequest validUpdateRequest;
    private ModuleResponse moduleResponse;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (ServletRequest) inv.getArgument(0),
                    (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        moduleId = UUID.randomUUID();

        validCreateRequest = new CreateModuleRequest(
                "CS101", "Intro to CS", "Description", 3,
                null, 30, "Semester 1", "2024/2025",
                null, null, "Room 101"
        );

        validUpdateRequest = new UpdateModuleRequest(
                "Updated CS", null, null, null, null, null, ModuleStatus.ACTIVE
        );

        moduleResponse = new ModuleResponse(
                moduleId, "CS101", "Intro to CS", "Description",
                3, null, 30, 0, ModuleStatus.ACTIVE,
                "Semester 1", "2024/2025", null, null,
                "Room 101", true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/modules — ADMIN creates module → 201")
    void createModule_AsAdmin_Returns201() throws Exception {
        when(moduleService.createModule(any(CreateModuleRequest.class))).thenReturn(moduleResponse);

        mockMvc.perform(post("/api/v1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CS101"));

        verify(moduleService).createModule(any(CreateModuleRequest.class));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/modules — TEACHER role → 403")
    void createModule_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/modules — blank code → 400")
    void createModule_BlankCode_Returns400() throws Exception {
        CreateModuleRequest bad = new CreateModuleRequest(
                "", "Name", null, 3,
                null, null, "Semester 1", "2024/2025",
                null, null, null
        );

        mockMvc.perform(post("/api/v1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/modules/{id} — found → 200")
    void getById_Exists_Returns200() throws Exception {
        when(moduleService.getModuleById(moduleId)).thenReturn(moduleResponse);

        mockMvc.perform(get("/api/v1/modules/" + moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CS101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/modules/{id} — not found → 404")
    void getById_NotFound_Returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(moduleService.getModuleById(unknownId))
                .thenThrow(new ResourceNotFoundException("Module", "id", unknownId));

        mockMvc.perform(get("/api/v1/modules/" + unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/modules/code/{code} — found → 200")
    void getByCode_Returns200() throws Exception {
        when(moduleService.getModuleByCode("CS101")).thenReturn(moduleResponse);

        mockMvc.perform(get("/api/v1/modules/code/CS101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CS101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/modules — returns paginated list → 200")
    void getAll_Returns200() throws Exception {
        Page<ModuleResponse> page = new PageImpl<>(List.of(moduleResponse));
        when(moduleService.getAllModules(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].code").value("CS101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/modules/status/ACTIVE — ADMIN → 200")
    void getByStatus_AsAdmin_Returns200() throws Exception {
        Page<ModuleResponse> page = new PageImpl<>(List.of(moduleResponse));
        when(moduleService.getModulesByStatus(eq(ModuleStatus.ACTIVE), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/modules/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/modules/status/ACTIVE — STUDENT role → 403")
    void getByStatus_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/modules/status/ACTIVE"))
                .andExpect(status().isForbidden());
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/modules/{id} — ADMIN updates → 200")
    void updateModule_Returns200() throws Exception {
        when(moduleService.updateModule(eq(moduleId), any(UpdateModuleRequest.class))).thenReturn(moduleResponse);

        mockMvc.perform(put("/api/v1/modules/" + moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/modules/{id} — ADMIN → 200")
    void deleteModule_Returns200() throws Exception {
        doNothing().when(moduleService).deleteModule(moduleId);

        mockMvc.perform(delete("/api/v1/modules/" + moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Module deleted successfully"));
    }

    // ── ENROLLMENT MUTATIONS ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/modules/{id}/enroll — STUDENT → 200")
    void incrementEnrollment_Returns200() throws Exception {
        doNothing().when(moduleService).incrementEnrollment(moduleId);

        mockMvc.perform(post("/api/v1/modules/" + moduleId + "/enroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/modules/{id}/unenroll — STUDENT → 200")
    void decrementEnrollment_Returns200() throws Exception {
        doNothing().when(moduleService).decrementEnrollment(moduleId);

        mockMvc.perform(post("/api/v1/modules/" + moduleId + "/unenroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
