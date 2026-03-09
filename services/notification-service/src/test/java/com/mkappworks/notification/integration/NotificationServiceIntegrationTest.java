package com.mkappworks.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.notification.dto.NotificationRequest;
import com.mkappworks.notification.model.Notification;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Notification Service Integration Tests")
class NotificationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db_test")
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

    private static final Long RECIPIENT_ID = 42L;

    private NotificationRequest buildRequest(Notification.NotificationType type) {
        return NotificationRequest.builder()
                .recipientId(RECIPIENT_ID)
                .recipientEmail("student@school.com")
                .subject("Test notification")
                .message("This is a test notification message")
                .type(type)
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /notifications/send — sends EMAIL notification → 200")
    void step1_SendEmailNotification_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(Notification.NotificationType.EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("EMAIL"))
                .andExpect(jsonPath("$.data.recipientId").value(RECIPIENT_ID));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /notifications/send — sends IN_APP notification → 200")
    void step2_SendInAppNotification_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(Notification.NotificationType.IN_APP))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("IN_APP"));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /notifications/recipient/{id} — returns 2 notifications")
    void step3_GetRecipientNotifications_Returns200WithTwoEntries() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/recipient/" + RECIPIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
