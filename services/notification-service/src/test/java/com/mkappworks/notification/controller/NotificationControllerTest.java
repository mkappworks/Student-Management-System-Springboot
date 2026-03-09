package com.mkappworks.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.common.exception.GlobalExceptionHandler;
import com.mkappworks.common.security.JwtAuthFilter;
import com.mkappworks.notification.config.SecurityConfig;
import com.mkappworks.notification.dto.NotificationRequest;
import com.mkappworks.notification.dto.NotificationResponse;
import com.mkappworks.notification.model.Notification;
import com.mkappworks.notification.service.NotificationService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("NotificationController Web Layer Tests")
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NotificationService notificationService;
    @MockBean private JwtAuthFilter jwtAuthFilter;

    private NotificationRequest validRequest;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (ServletRequest) inv.getArgument(0),
                    (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        validRequest = NotificationRequest.builder()
                .recipientId(1L)
                .recipientEmail("student@school.com")
                .subject("Test Subject")
                .message("Test message body")
                .type(Notification.NotificationType.EMAIL)
                .build();

        notificationResponse = NotificationResponse.builder()
                .id(1L).recipientId(1L)
                .recipientEmail("student@school.com")
                .subject("Test Subject")
                .type(Notification.NotificationType.EMAIL)
                .status(Notification.NotificationStatus.SENT)
                .build();
    }

    // ── SEND ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/notifications/send — TEACHER sends → 200")
    void send_AsTeacher_Returns200() throws Exception {
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(notificationResponse);

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"));

        verify(notificationService).sendNotification(any(NotificationRequest.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/notifications/send — STUDENT role → 403")
    void send_AsStudent_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /api/v1/notifications/send — blank message → 400")
    void send_BlankMessage_Returns400() throws Exception {
        NotificationRequest bad = NotificationRequest.builder()
                .recipientId(1L)
                .recipientEmail("student@school.com")
                .subject("Subject")
                .message("")
                .build();

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ── GET BY RECIPIENT ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/notifications/recipient/{id} — returns list → 200")
    void getByRecipient_Returns200() throws Exception {
        when(notificationService.getRecipientNotifications(1L)).thenReturn(List.of(notificationResponse));

        mockMvc.perform(get("/api/v1/notifications/recipient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].recipientId").value(1));
    }
}
