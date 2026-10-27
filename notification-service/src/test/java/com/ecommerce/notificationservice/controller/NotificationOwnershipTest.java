package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.mockito.Mockito.*;

class NotificationOwnershipTest {
    @Test void customerReadsOnlyNotificationsForTheirJwtSubject() {
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("keycloak-user-A").build();
        controller.getAllNotifications(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("USER"))));
        verify(service).getNotificationsForRecipient("keycloak-user-A");
        verify(service, never()).getAllNotifications();
    }

    @Test void customerCannotFetchAnotherRecipientsNotification() {
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("keycloak-user-A").build();
        controller.getNotificationById(44L, new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("USER"))));
        verify(service).getNotificationByIdForRecipient(44L, "keycloak-user-A");
        verify(service, never()).getNotificationById(44L);
    }

    @Test void administratorsRetainNotificationManagementReadAccess() {
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);
        var admin = new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ADMIN")));
        controller.getAllNotifications(admin);
        verify(service).getAllNotifications();
    }
}
