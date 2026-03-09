package com.mkappworks.moduleservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.moduleservice.dto.ModuleDtos.CreateModuleRequest;
import com.mkappworks.moduleservice.dto.ModuleDtos.UpdateModuleRequest;
import com.mkappworks.moduleservice.entity.ModuleStatus;
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
@DisplayName("Module Service Integration Tests")
class ModuleServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("module_db_test")
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

    private static String createdModuleId;
    private static final String MODULE_CODE = "CS101";

    private CreateModuleRequest buildCreateRequest(String code) {
        return new CreateModuleRequest(
                code, "Intro to CS", "Fundamentals", 3,
                null, 30, "Semester 1", "2024/2025",
                null, null, "Room 101"
        );
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /modules — creates module and returns 201")
    void step1_CreateModule_Returns201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(MODULE_CODE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(MODULE_CODE))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdModuleId = objectMapper.readTree(body).path("data").path("id").asText();
        assertThat(createdModuleId).isNotBlank();
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /modules — duplicate code returns 409")
    void step2_CreateModule_DuplicateCode_Returns409() throws Exception {
        mockMvc.perform(post("/api/v1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(MODULE_CODE))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /modules/{id} — returns module data")
    void step3_GetModuleById_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/modules/" + createdModuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdModuleId))
                .andExpect(jsonPath("$.data.code").value(MODULE_CODE));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /modules/code/{code} — returns module by code")
    void step4_GetModuleByCode_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/modules/code/" + MODULE_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(MODULE_CODE));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /modules — returns page with 1 module")
    void step5_GetAllModules_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /modules/status/ACTIVE — returns active modules")
    void step6_GetModulesByStatus_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/modules/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /modules/{id} — updates module successfully")
    void step7_UpdateModule_Returns200() throws Exception {
        UpdateModuleRequest update = new UpdateModuleRequest(
                "Updated CS", "Updated description", null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/modules/" + createdModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated CS"));
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /modules/{id}/enroll — increments enrollment")
    void step8_IncrementEnrollment_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/modules/" + createdModuleId + "/enroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /modules/{id}/unenroll — decrements enrollment")
    void step9_DecrementEnrollment_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/modules/" + createdModuleId + "/unenroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /modules/{id} — deletes module successfully")
    void step10_DeleteModule_Returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/modules/" + createdModuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
