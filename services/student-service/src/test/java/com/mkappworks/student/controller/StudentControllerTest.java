package com.mkappworks.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.student.dto.StudentRequest;
import com.mkappworks.student.dto.StudentResponse;
import com.mkappworks.student.exception.ResourceNotFoundException;
import com.mkappworks.student.model.Student;
import com.mkappworks.student.repository.UserRepository;
import com.mkappworks.student.security.JwtAuthenticationFilter;
import com.mkappworks.student.security.JwtService;
import com.mkappworks.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer (MockMvc) tests for StudentController.
 *
 * Strategy: mock every bean SecurityConfig and ApplicationConfig need so
 * Spring Security's filter chain (CSRF + role checks) runs normally without
 * starting a real JPA / Eureka context.
 *
 *   JwtAuthenticationFilter  → @MockBean  (SecurityConfig constructor arg)
 *   AuthenticationProvider   → @MockBean  (SecurityConfig constructor arg)
 *   UserDetailsService       → @MockBean  (overrides ApplicationConfig.userDetailsService())
 *   UserRepository           → @MockBean  (ApplicationConfig @RequiredArgsConstructor)
 *   JwtService               → @MockBean  (JwtAuthenticationFilter constructor arg — unused at runtime)
 */
@WebMvcTest(controllers = StudentController.class)
@DisplayName("StudentController Web Layer Tests")
class StudentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StudentService           studentService;
    @MockBean private JwtAuthenticationFilter  jwtAuthenticationFilter;
    @MockBean private AuthenticationProvider   authenticationProvider;
    @MockBean private UserDetailsService       userDetailsService;
    @MockBean private UserRepository           userRepository;
    @MockBean private JwtService               jwtService;

    private StudentRequest  validRequest;
    private StudentResponse studentResponse;

    @BeforeEach
    void setUp() {
        validRequest = StudentRequest.builder()
                .firstName("Jane").lastName("Smith")
                .email("jane.smith@test.com")
                .programme("Mathematics").yearOfStudy(2)
                .build();

        studentResponse = StudentResponse.builder()
                .id(1L).studentNumber("STU2412345")
                .firstName("Jane").lastName("Smith")
                .email("jane.smith@test.com")
                .programme("Mathematics").yearOfStudy(2)
                .status(Student.StudentStatus.ACTIVE).gpa(3.5)
                .build();
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/students — ADMIN creates student → 201")
    void createStudent_AsAdmin_Returns201() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class))).thenReturn(studentResponse);

        mockMvc.perform(post("/api/v1/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.email").value("jane.smith@test.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(studentService).createStudent(any(StudentRequest.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/students — STUDENT role → 403")
    void createStudent_AsStudent_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/students — blank firstName → 400")
    void createStudent_BlankFirstName_Returns400() throws Exception {
        StudentRequest bad = StudentRequest.builder()
                .firstName("").lastName("Smith")
                .email("valid@test.com").programme("CS").build();

        mockMvc.perform(post("/api/v1/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/students — invalid email format → 400")
    void createStudent_InvalidEmail_Returns400() throws Exception {
        StudentRequest bad = StudentRequest.builder()
                .firstName("Test").lastName("User")
                .email("not-an-email").programme("CS").build();

        mockMvc.perform(post("/api/v1/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/students/{id} — existing ID → 200 with student data")
    void getStudentById_Exists_Returns200() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(studentResponse);

        mockMvc.perform(get("/api/v1/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.studentNumber").value("STU2412345"))
                .andExpect(jsonPath("$.data.programme").value("Mathematics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/students/{id} — unknown ID → 404")
    void getStudentById_NotFound_Returns404() throws Exception {
        when(studentService.getStudentById(999L))
                .thenThrow(new ResourceNotFoundException("Student", "id", 999L));

        mockMvc.perform(get("/api/v1/students/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/students — returns paginated list")
    void getAllStudents_Returns200WithPage() throws Exception {
        Page<StudentResponse> page = new PageImpl<>(List.of(studentResponse));
        when(studentService.getAllStudents(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("jane.smith@test.com"));
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/students/{id} — valid update → 200")
    void updateStudent_ValidRequest_Returns200() throws Exception {
        when(studentService.updateStudent(eq(1L), any(StudentRequest.class))).thenReturn(studentResponse);

        mockMvc.perform(put("/api/v1/students/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/v1/students/{id}/status — valid status change → 200")
    void updateStudentStatus_Returns200() throws Exception {
        studentResponse.setStatus(Student.StudentStatus.INACTIVE);
        when(studentService.updateStudentStatus(1L, Student.StudentStatus.INACTIVE)).thenReturn(studentResponse);

        mockMvc.perform(patch("/api/v1/students/1/status")
                        .with(csrf())
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/students/{id} — existing student → 200 with message")
    void deleteStudent_Exists_Returns200() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/api/v1/students/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /api/v1/students/{id} — TEACHER role → 403")
    void deleteStudent_AsTeacher_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/students/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
