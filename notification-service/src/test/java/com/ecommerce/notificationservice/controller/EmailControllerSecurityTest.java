package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.PaymentService;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.dto.PaymentDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(EmailControllerSecurityTest.TestConfig.class)
class EmailControllerSecurityTest {
    @Autowired EmailController controller;
    @Autowired EmailService emailService;
    @Autowired PaymentService paymentService;
    @Autowired NotificationService notificationService;
    @Autowired PaymentController paymentController;
    @Autowired NotificationController notificationController;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void userCannotSendArbitraryEmail() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class, () -> controller.sendSimpleMail(new EmailDetails()));
        verifyNoInteractions(emailService);
    }

    @Test void userCannotUseMultipartEmailOrAdministrativeNotificationEndpoints() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class, () -> controller.sendMail(null, "victim@example.test", new String[0], "subject", "body"));
        assertThrows(AccessDeniedException.class, () -> notificationController.saveNotification(new Notification()));
        assertThrows(AccessDeniedException.class, () -> notificationController.deleteNotification(1L));
        assertThrows(AccessDeniedException.class, () -> paymentController.getAllPayments());
        assertThrows(AccessDeniedException.class, () -> paymentController.savePayment(new PaymentDto()));
        assertThrows(AccessDeniedException.class, () -> paymentController.deletePayment(1L));
        verifyNoInteractions(emailService, paymentService, notificationService);
    }

    @Test void unauthenticatedCallerCannotSendEmail() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.sendSimpleMail(new EmailDetails()));
        verifyNoInteractions(emailService);
    }

    @Test void adminCanUseAuthorizedEmailWorkflow() {
        authenticate("ADMIN");
        when(emailService.sendSimpleMail(any())).thenReturn("Mail Sent Successfully");
        controller.sendSimpleMail(new EmailDetails());
        verify(emailService).sendSimpleMail(any());
        when(emailService.sendMail(any(), anyString(), any(), anyString(), anyString())).thenReturn("Email sent successfully");
        controller.sendMail(null, "to@example.test", new String[0], "subject", "body");
        verify(emailService).sendMail(null, "to@example.test", new String[0], "subject", "body");
    }

    private void authenticate(String role) {
        var auth = new UsernamePasswordAuthenticationToken("caller", "n/a", java.util.List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean EmailService emailService() { return mock(EmailService.class); }
        @Bean PaymentService paymentService() { return mock(PaymentService.class); }
        @Bean NotificationService notificationService() { return mock(NotificationService.class); }
        @Bean EmailController emailController(EmailService service) { return new EmailController(service); }
        @Bean PaymentController paymentController(PaymentService service) { return new PaymentController(service); }
        @Bean NotificationController notificationController(NotificationService service) { return new NotificationController(service); }
    }
}
