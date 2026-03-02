package com.mkappworks.notification.dto;

import com.mkappworks.notification.model.Notification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRequest {
    @NotNull private Long recipientId;
    @Email @NotBlank private String recipientEmail;
    @NotBlank private String subject;
    @NotBlank private String message;
    private Notification.NotificationType type;
}
