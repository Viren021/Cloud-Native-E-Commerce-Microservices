package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    List<Notification> getAllNotifications();
    List<Notification> getNotificationsForRecipient(String recipientId);
    Optional<Notification> getNotificationById(Long id);
    Optional<Notification> getNotificationByIdForRecipient(Long id, String recipientId);
    Notification saveNotification(Notification notification);
    void deleteNotificationById(Long id);
}
