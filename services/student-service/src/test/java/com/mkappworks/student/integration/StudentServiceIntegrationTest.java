package com.mkappworks.student.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.student.dto.AuthRequest;
import com.mkappworks.student.dto.RegisterRequest;
import com.mkappworks.student.dto.StudentRequest;
import com.mkappworks.student.model.Student;
import com.mkappworks.student.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Student Service Integration Tests")
class StudentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("student_db_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Shared state across ordered tests
    private static String adminToken;
    private static Long createdStudentId;

    // ── Auth setup ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Register admin user via student-service auth endpoint")
    void step1_RegisterAdmin() throws Exception {
        var request = RegisterRequest.builder()
                .firstName("Test").lastName("Admin")
                .email("testadmin@sms.com")
                .password("Admin@1234")
                .role(User.Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Login and capture JWT token")
    void step2_LoginAndCaptureToken() throws Exception {
        var authRequest = AuthRequest.builder()
                .email("testadmin@sms.com")
                .password("Admin@1234")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
        assertThat(adminToken).isNotBlank();
    }

    // ── Unauthenticated access ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("GET /students without token → 401 Unauthorized")
    void step3_GetStudents_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized());
    }

    // ── Student CRUD ──────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /students with admin token → 201 with student data")
    void step4_CreateStudent_Returns201() throws Exception {
        var request = StudentRequest.builder()
                .firstName("Alice").lastName("Johnson")
                .email("alice.johnson@student.com")
                .phone("9876543210")
                .programme("Computer Science")
                .yearOfStudy(1)
                .gender(Student.Gender.FEMALE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.studentNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdStudentId = objectMapper.readTree(body).path("data").path("id").asLong();
        assertThat(createdStudentId).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("POST /students with duplicate email → 409 Conflict")
    void step5_CreateStudent_DuplicateEmail_Returns409() throws Exception {
        var request = StudentRequest.builder()
                .firstName("Bob").lastName("Smith")
                .email("alice.johnson@student.com") // duplicate
                .programme("Physics").yearOfStudy(1)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    @DisplayName("GET /students/{id} → 200 with correct student")
    void step6_GetStudentById_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + createdStudentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdStudentId))
                .andExpect(jsonPath("$.data.email").value("alice.johnson@student.com"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /students?page=0&size=10 → 200 with list")
    void step7_GetAllStudents_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("GET /students/search?query=Alice → 200 with results")
    void step8_SearchStudents_ReturnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/students/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("query", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("PATCH /students/{id}/status → 200 with updated status")
    void step9_UpdateStudentStatus_Returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/students/" + createdStudentId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /students/{id} → 200 with success message")
    void step10_DeleteStudent_Returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/students/" + createdStudentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student deleted successfully"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /students/{id} after delete → 404 Not Found")
    void step11_GetDeletedStudent_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + createdStudentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
