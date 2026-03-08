package com.mkappworks.notification.controller;

import com.mkappworks.notification.dto.NotificationRequest;
import com.mkappworks.notification.dto.NotificationResponse;
import com.mkappworks.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getRecipientNotifications(recipientId));
    }
}
