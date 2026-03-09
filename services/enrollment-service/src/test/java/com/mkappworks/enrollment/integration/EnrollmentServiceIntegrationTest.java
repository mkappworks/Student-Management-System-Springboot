package com.mkappworks.enrollment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.enrollment.dto.EnrollmentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
@DisplayName("Enrollment Service Integration Tests")
class EnrollmentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("enrollment_db_test")
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

    private static Long createdEnrollmentId;

    private EnrollmentRequest buildRequest() {
        return EnrollmentRequest.builder()
                .studentId(5L).moduleId(10L)
                .academicYear("2024/2025").semester(1)
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /enrollments — enrolls student and returns 201")
    void step1_Enroll_Returns201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.studentId").value(5))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdEnrollmentId = objectMapper.readTree(body).path("data").path("id").asLong();
        assertThat(createdEnrollmentId).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /enrollments — duplicate enrollment returns 409")
    void step2_Enroll_Duplicate_Returns409() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /enrollments/{id} — returns enrollment data")
    void step3_GetEnrollment_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/" + createdEnrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdEnrollmentId))
                .andExpect(jsonPath("$.data.moduleId").value(10));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /enrollments/student/{studentId} — returns list")
    void step4_GetStudentEnrollments_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/student/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentId").value(5));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "STUDENT")
    @DisplayName("PATCH /enrollments/{id}/drop — drops enrollment successfully")
    void step5_DropEnrollment_Returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/" + createdEnrollmentId + "/drop")
                        .param("reason", "Schedule conflict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /enrollments/{id} after drop — confirms DROPPED status")
    void step6_GetDroppedEnrollment_ConfirmsStatus() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/" + createdEnrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"));
    }
}
