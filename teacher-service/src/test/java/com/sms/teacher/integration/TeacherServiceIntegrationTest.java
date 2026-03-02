package com.sms.teacher.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.teacher.dto.TeacherRequest;
import com.sms.teacher.model.Teacher;
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
@DisplayName("Teacher Service Integration Tests")
class TeacherServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("teacher_db_test")
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

    private static Long createdTeacherId;

    private TeacherRequest buildRequest(String email) {
        return TeacherRequest.builder()
                .firstName("Alice").lastName("Wang")
                .email(email)
                .department("Mathematics")
                .qualification("PhD")
                .specialization("Algebra")
                .yearsOfExperience(8)
                .employmentType(Teacher.EmploymentType.FULL_TIME)
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teachers — creates teacher and returns 201")
    void step1_CreateTeacher_Returns201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("alice.wang@school.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.employeeId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdTeacherId = objectMapper.readTree(body).path("id").asLong();
        assertThat(createdTeacherId).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teachers — duplicate email returns 409")
    void step2_CreateTeacher_DuplicateEmail_Returns409() throws Exception {
        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("alice.wang@school.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teachers/{id} — returns teacher data")
    void step3_GetTeacher_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/" + createdTeacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTeacherId))
                .andExpect(jsonPath("$.department").value("Mathematics"));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teachers — returns list")
    void step4_GetAllTeachers_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teachers/search?query=Alice — returns matching teachers")
    void step5_SearchTeachers_ReturnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/search").param("query", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /teachers/{id} — updates teacher successfully")
    void step6_UpdateTeacher_Returns200() throws Exception {
        TeacherRequest update = buildRequest("alice.wang.updated@school.com");
        mockMvc.perform(put("/api/v1/teachers/" + createdTeacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice.wang.updated@school.com"));
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /teachers/{id} — deletes teacher")
    void step7_DeleteTeacher_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/" + createdTeacherId))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teachers/{id} after delete — returns 404")
    void step8_GetDeletedTeacher_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/" + createdTeacherId))
                .andExpect(status().isNotFound());
    }
}
