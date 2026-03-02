package com.sms.notification.service;

import com.sms.notification.dto.NotificationRequest;
import com.sms.notification.dto.NotificationResponse;
import com.sms.notification.model.Notification;
import com.sms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .recipientEmail(request.getRecipientEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : Notification.NotificationType.IN_APP)
                .status(Notification.NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // Simulate sending (in real app integrate with email/SMS provider)
        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        log.info("Notification sent to: {}", request.getRecipientEmail());

        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecipientNotifications(Long recipientId) {
        return notificationRepository.findByRecipientId(recipientId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).recipientId(n.getRecipientId())
                .recipientEmail(n.getRecipientEmail()).subject(n.getSubject())
                .type(n.getType()).status(n.getStatus())
                .createdAt(n.getCreatedAt()).sentAt(n.getSentAt()).build();
    }
}
