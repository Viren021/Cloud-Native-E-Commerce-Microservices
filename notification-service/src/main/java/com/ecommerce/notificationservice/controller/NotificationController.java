package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAllNotifications(Authentication authentication) {
        if (isAdministrator(authentication)) return notificationService.getAllNotifications();
        return notificationService.getNotificationsForRecipient(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id, Authentication authentication) {
        Optional<Notification> notification = isAdministrator(authentication)
                ? notificationService.getNotificationById(id)
                : notificationService.getNotificationByIdForRecipient(id, authentication.getName());
        return notification.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Notification> saveNotification(@RequestBody Notification notification) {
        Notification savedNotification = notificationService.saveNotification(notification);
        return new ResponseEntity<>(savedNotification, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Boolean> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotificationById(id);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority()));
    }

}
