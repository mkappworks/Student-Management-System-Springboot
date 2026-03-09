package com.mkappworks.teacher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.common.security.JwtAuthFilter;
import com.mkappworks.teacher.config.SecurityConfig;
import com.mkappworks.teacher.dto.TeacherRequest;
import com.mkappworks.teacher.dto.TeacherResponse;
import com.mkappworks.teacher.model.Teacher;
import com.mkappworks.teacher.service.TeacherService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TeacherController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TeacherController Web Layer Tests")
class TeacherControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TeacherService teacherService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private TeacherRequest validRequest;
    private TeacherResponse teacherResponse;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (ServletRequest) inv.getArgument(0),
                    (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        validRequest = TeacherRequest.builder()
                .firstName("Alice").lastName("Wang")
                .email("alice.wang@school.com")
                .department("Mathematics")
                .qualification("PhD")
                .build();

        teacherResponse = TeacherResponse.builder()
                .id(1L).employeeId("EMP001")
                .firstName("Alice").lastName("Wang")
                .email("alice.wang@school.com")
                .department("Mathematics")
                .status(Teacher.TeacherStatus.ACTIVE)
                .employmentType(Teacher.EmploymentType.FULL_TIME)
                .build();
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/teachers — ADMIN creates teacher → 201")
    void create_AsAdmin_Returns201() throws Exception {
        when(teacherService.createTeacher(any(TeacherRequest.class))).thenReturn(teacherResponse);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(teacherService).createTeacher(any(TeacherRequest.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/teachers — STUDENT role → 403")
    void create_AsStudent_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(teacherService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/teachers — blank firstName → 400")
    void create_BlankFirstName_Returns400() throws Exception {
        TeacherRequest bad = TeacherRequest.builder()
                .firstName("").lastName("Wang")
                .email("alice@school.com").department("Math").build();

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/teachers/{id} — found → 200")
    void getById_Exists_Returns200() throws Exception {
        when(teacherService.getTeacherById(1L)).thenReturn(teacherResponse);

        mockMvc.perform(get("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.department").value("Mathematics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/teachers/999 — not found → 404")
    void getById_NotFound_Returns404() throws Exception {
        when(teacherService.getTeacherById(999L))
                .thenThrow(new ResourceNotFoundException("Teacher", "id", 999L));

        mockMvc.perform(get("/api/v1/teachers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/teachers — paginated → 200")
    void getAll_Returns200WithPage() throws Exception {
        Page<TeacherResponse> page = new PageImpl<>(List.of(teacherResponse));
        when(teacherService.getAllTeachers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("alice.wang@school.com"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/teachers — STUDENT role → 403")
    void getAll_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/teachers/search?query=Alice — 200")
    void search_Returns200() throws Exception {
        Page<TeacherResponse> page = new PageImpl<>(List.of(teacherResponse));
        when(teacherService.searchTeachers(eq("Alice"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers/search").param("query", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/teachers/{id} — valid update → 200")
    void update_Returns200() throws Exception {
        when(teacherService.updateTeacher(eq(1L), any(TeacherRequest.class))).thenReturn(teacherResponse);

        mockMvc.perform(put("/api/v1/teachers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/teachers/{id} — ADMIN → 200")
    void delete_AsAdmin_Returns200() throws Exception {
        doNothing().when(teacherService).deleteTeacher(1L);

        mockMvc.perform(delete("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Teacher deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /api/v1/teachers/{id} — TEACHER role → 403")
    void delete_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/1"))
                .andExpect(status().isForbidden());
    }
}
