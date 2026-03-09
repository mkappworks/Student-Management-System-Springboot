package com.mkappworks.grade.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.grade.dto.GradeRequest;
import com.mkappworks.grade.model.Grade;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Grade Service Integration Tests")
class GradeServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("grade_db_test")
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

    private static Long createdGradeId;

    private GradeRequest buildRequest() {
        return GradeRequest.builder()
                .studentId(10L).moduleId(20L).teacherId(30L)
                .score(new BigDecimal("78"))
                .maxScore(new BigDecimal("100"))
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .remarks("Good performance")
                .semester(1)
                .academicYear("2024/2025")
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /grades — assigns grade and returns 201")
    void step1_AssignGrade_Returns201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assessmentType").value("MIDTERM"))
                .andExpect(jsonPath("$.data.letterGrade").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdGradeId = objectMapper.readTree(body).path("data").path("id").asLong();
        assertThat(createdGradeId).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /grades/{id} — returns grade data")
    void step2_GetGrade_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/grades/" + createdGradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdGradeId))
                .andExpect(jsonPath("$.data.studentId").value(10))
                .andExpect(jsonPath("$.data.semester").value(1));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /grades/student/{studentId} — returns list with grade")
    void step3_GetStudentGrades_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/grades/student/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentId").value(10));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "TEACHER")
    @DisplayName("PUT /grades/{id} — updates grade successfully")
    void step4_UpdateGrade_Returns200() throws Exception {
        GradeRequest update = GradeRequest.builder()
                .studentId(10L).moduleId(20L).teacherId(30L)
                .score(new BigDecimal("90"))
                .maxScore(new BigDecimal("100"))
                .assessmentType(Grade.AssessmentType.MIDTERM)
                .remarks("Excellent")
                .semester(1)
                .academicYear("2024/2025")
                .build();

        mockMvc.perform(put("/api/v1/grades/" + createdGradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remarks").value("Excellent"));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /grades/{id} — deletes grade successfully")
    void step5_DeleteGrade_Returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/grades/" + createdGradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /grades/{id} after delete — returns 404")
    void step6_GetDeletedGrade_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/grades/" + createdGradeId))
                .andExpect(status().isNotFound());
    }
}
