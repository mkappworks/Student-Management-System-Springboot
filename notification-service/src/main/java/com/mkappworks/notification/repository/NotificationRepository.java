package com.mkappworks.notification.repository;

import com.mkappworks.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientId(Long recipientId);
    List<Notification> findByStatus(Notification.NotificationStatus status);
    List<Notification> findByRecipientIdAndStatus(Long recipientId, Notification.NotificationStatus status);
}
