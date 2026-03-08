package com.mkappworks.notification.dto;

import com.mkappworks.notification.model.Notification;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String recipientEmail;
    private String subject;
    private Notification.NotificationType type;
    private Notification.NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
