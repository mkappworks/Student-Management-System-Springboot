package com.mkappworks.grade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.common.security.JwtAuthFilter;
import com.mkappworks.grade.config.SecurityConfig;
import com.mkappworks.grade.dto.GradeRequest;
import com.mkappworks.grade.dto.GradeResponse;
import com.mkappworks.grade.model.Grade;
import com.mkappworks.grade.service.GradeService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("GradeController Web Layer Tests")
class GradeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private GradeService gradeService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private GradeRequest validRequest;
    private GradeResponse gradeResponse;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (ServletRequest) inv.getArgument(0),
                    (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        validRequest = GradeRequest.builder()
                .studentId(1L).moduleId(1L).teacherId(1L)
                .score(new BigDecimal("85"))
                .maxScore(new BigDecimal("100"))
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .build();

        gradeResponse = GradeResponse.builder()
                .id(1L).studentId(1L).moduleId(1L).teacherId(1L)
                .score(new BigDecimal("85")).maxScore(new BigDecimal("100"))
                .percentage(new BigDecimal("85.00")).letterGrade("A")
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .build();
    }

    // ── ASSIGN ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/grades — TEACHER assigns grade → 201")
    void assignGrade_AsTeacher_Returns201() throws Exception {
        when(gradeService.assignGrade(any(GradeRequest.class))).thenReturn(gradeResponse);

        mockMvc.perform(post("/api/v1/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.letterGrade").value("A"));

        verify(gradeService).assignGrade(any(GradeRequest.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/grades — STUDENT role → 403")
    void assignGrade_AsStudent_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gradeService);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/grades — null score → 400")
    void assignGrade_NullScore_Returns400() throws Exception {
        GradeRequest bad = GradeRequest.builder()
                .studentId(1L).moduleId(1L).teacherId(1L)
                .maxScore(new BigDecimal("100"))
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .build();

        mockMvc.perform(post("/api/v1/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/grades/{id} — found → 200")
    void getGrade_Exists_Returns200() throws Exception {
        when(gradeService.getGradeById(1L)).thenReturn(gradeResponse);

        mockMvc.perform(get("/api/v1/grades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.assessmentType").value("MIDTERM"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/grades/999 — not found → 404")
    void getGrade_NotFound_Returns404() throws Exception {
        when(gradeService.getGradeById(999L))
                .thenThrow(new ResourceNotFoundException("Grade", "id", 999L));

        mockMvc.perform(get("/api/v1/grades/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/grades/student/{studentId} — returns list → 200")
    void getStudentGrades_Returns200() throws Exception {
        when(gradeService.getStudentGrades(1L)).thenReturn(List.of(gradeResponse));

        mockMvc.perform(get("/api/v1/grades/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentId").value(1));
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("PUT /api/v1/grades/{id} — TEACHER updates → 200")
    void updateGrade_AsTeacher_Returns200() throws Exception {
        when(gradeService.updateGrade(eq(1L), any(GradeRequest.class))).thenReturn(gradeResponse);

        mockMvc.perform(put("/api/v1/grades/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/grades/{id} — ADMIN → 200")
    void deleteGrade_AsAdmin_Returns200() throws Exception {
        doNothing().when(gradeService).deleteGrade(1L);

        mockMvc.perform(delete("/api/v1/grades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Grade deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /api/v1/grades/{id} — TEACHER role → 403")
    void deleteGrade_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/grades/1"))
                .andExpect(status().isForbidden());
    }
}
