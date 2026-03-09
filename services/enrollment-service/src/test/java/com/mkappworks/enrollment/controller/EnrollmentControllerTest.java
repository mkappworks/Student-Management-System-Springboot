package com.mkappworks.enrollment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.common.security.JwtAuthFilter;
import com.mkappworks.enrollment.config.SecurityConfig;
import com.mkappworks.enrollment.dto.EnrollmentRequest;
import com.mkappworks.enrollment.dto.EnrollmentResponse;
import com.mkappworks.enrollment.model.Enrollment;
import com.mkappworks.enrollment.service.EnrollmentService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EnrollmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("EnrollmentController Web Layer Tests")
class EnrollmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private EnrollmentService enrollmentService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private EnrollmentRequest validRequest;
    private EnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (ServletRequest) inv.getArgument(0),
                    (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        validRequest = EnrollmentRequest.builder()
                .studentId(1L).moduleId(1L)
                .academicYear("2024/2025").semester(1)
                .build();

        enrollmentResponse = EnrollmentResponse.builder()
                .id(1L).studentId(1L).moduleId(1L)
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .academicYear("2024/2025").semester(1)
                .enrollmentDate(LocalDate.now())
                .build();
    }

    // ── ENROLL ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/enrollments — STUDENT enrolls → 201")
    void enroll_AsStudent_Returns201() throws Exception {
        when(enrollmentService.enroll(any(EnrollmentRequest.class))).thenReturn(enrollmentResponse);

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ENROLLED"));

        verify(enrollmentService).enroll(any(EnrollmentRequest.class));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/enrollments — TEACHER role → 403")
    void enroll_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/enrollments — null studentId → 400")
    void enroll_NullStudentId_Returns400() throws Exception {
        EnrollmentRequest bad = EnrollmentRequest.builder()
                .moduleId(1L).academicYear("2024/2025").build();

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/enrollments/{id} — found → 200")
    void getById_Exists_Returns200() throws Exception {
        when(enrollmentService.getEnrollmentById(1L)).thenReturn(enrollmentResponse);

        mockMvc.perform(get("/api/v1/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.studentId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/enrollments/999 — not found → 404")
    void getById_NotFound_Returns404() throws Exception {
        when(enrollmentService.getEnrollmentById(999L))
                .thenThrow(new ResourceNotFoundException("Enrollment", "id", 999L));

        mockMvc.perform(get("/api/v1/enrollments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/enrollments/student/{studentId} — returns list → 200")
    void getStudentEnrollments_Returns200() throws Exception {
        when(enrollmentService.getStudentEnrollments(1L)).thenReturn(List.of(enrollmentResponse));

        mockMvc.perform(get("/api/v1/enrollments/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].moduleId").value(1));
    }

    // ── DROP ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("PATCH /api/v1/enrollments/{id}/drop — STUDENT drops → 200")
    void drop_AsStudent_Returns200() throws Exception {
        EnrollmentResponse dropped = EnrollmentResponse.builder()
                .id(1L).studentId(1L).moduleId(1L)
                .status(Enrollment.EnrollmentStatus.DROPPED)
                .build();
        when(enrollmentService.dropEnrollment(eq(1L), any())).thenReturn(dropped);

        mockMvc.perform(patch("/api/v1/enrollments/1/drop")
                        .param("reason", "Schedule conflict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("PATCH /api/v1/enrollments/{id}/drop — TEACHER role → 403")
    void drop_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/1/drop"))
                .andExpect(status().isForbidden());
    }
}
