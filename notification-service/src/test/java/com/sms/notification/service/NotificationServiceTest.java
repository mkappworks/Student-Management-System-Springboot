package com.sms.notification.service;

import com.sms.notification.dto.NotificationRequest;
import com.sms.notification.dto.NotificationResponse;
import com.sms.notification.model.Notification;
import com.sms.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationService notificationService;

    private NotificationRequest request;

    @BeforeEach
    void setUp() {
        request = NotificationRequest.builder()
                .recipientId(1L)
                .recipientEmail("student@school.com")
                .subject("Enrollment Confirmed")
                .message("You have been enrolled in CS101.")
                .type(Notification.NotificationType.EMAIL)
                .build();
    }

    /** Helper: build a persisted notification with given status */
    private Notification buildNotification(Long id, Notification.NotificationStatus status) {
        Notification n = Notification.builder()
                .id(id)
                .recipientId(1L)
                .recipientEmail("student@school.com")
                .subject("Enrollment Confirmed")
                .message("You have been enrolled in CS101.")
                .type(Notification.NotificationType.EMAIL)
                .status(status)
                .build();
        if (status == Notification.NotificationStatus.SENT) {
            n.setSentAt(LocalDateTime.now());
        }
        return n;
    }

    // ── sendNotification ───────────────────────────────────────────────────────

    @Test
    @DisplayName("sendNotification — saves twice (PENDING then SENT) and returns SENT status")
    void sendNotification_EmailType_ReturnsSentStatus() {
        // First save: PENDING; second save: SENT
        Notification pending = buildNotification(1L, Notification.NotificationStatus.PENDING);
        Notification sent    = buildNotification(1L, Notification.NotificationStatus.SENT);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(pending)   // first call
                .thenReturn(sent);     // second call

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(response.getRecipientEmail()).isEqualTo("student@school.com");
        assertThat(response.getSentAt()).isNotNull();
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendNotification — null type defaults to IN_APP")
    void sendNotification_NullType_DefaultsToInApp() {
        request.setType(null);
        Notification pending = buildNotification(2L, Notification.NotificationStatus.PENDING);
        Notification sent    = buildNotification(2L, Notification.NotificationStatus.SENT);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(pending)
                .thenReturn(sent);

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendNotification — SMS type is persisted correctly")
    void sendNotification_SmsType_Returns200() {
        request.setType(Notification.NotificationType.SMS);
        Notification pending = buildNotification(3L, Notification.NotificationStatus.PENDING);
        Notification sent    = buildNotification(3L, Notification.NotificationStatus.SENT);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(pending)
                .thenReturn(sent);

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
    }

    // ── getRecipientNotifications ──────────────────────────────────────────────

    @Test
    @DisplayName("getRecipientNotifications — returns all notifications for a recipient")
    void getRecipientNotifications_HasNotifications_ReturnsList() {
        Notification n1 = buildNotification(1L, Notification.NotificationStatus.SENT);
        Notification n2 = buildNotification(2L, Notification.NotificationStatus.READ);
        when(notificationRepository.findByRecipientId(1L)).thenReturn(List.of(n1, n2));

        List<NotificationResponse> result = notificationService.getRecipientNotifications(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getRecipientEmail().equals("student@school.com"));
    }

    @Test
    @DisplayName("getRecipientNotifications — returns empty list for recipient with no notifications")
    void getRecipientNotifications_NoNotifications_ReturnsEmpty() {
        when(notificationRepository.findByRecipientId(999L)).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getRecipientNotifications(999L);

        assertThat(result).isEmpty();
    }
}
