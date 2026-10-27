package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.entity.EmailDelivery;
import com.ecommerce.notificationservice.repository.EmailDeliveryRepository;
import com.ecommerce.notificationservice.service.EmailService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailDeliveryServiceTest {
    @Test void deliversFirstOnboardingEventAndStoresPersistentDedupeKey() {
        EmailDeliveryRepository repository = mock(EmailDeliveryRepository.class);
        EmailService email = mock(EmailService.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(email.sendSimpleMail(any())).thenReturn("Mail Sent Successfully");
        String message = new Gson().toJson(EmailDetails.builder().recipient("user@example.test").build());
        new EmailDeliveryService(repository, email).deliverOnboardingEvent(message);
        verify(repository).saveAndFlush(argThat(row -> "PENDING".equals(row.getStatus())));
        verify(repository).save(argThat(row -> "DELIVERED".equals(row.getStatus())));
        verify(email).sendSimpleMail(any());
    }

    @Test void replayedOnboardingEventDoesNotSendDuplicateEmail() {
        EmailDeliveryRepository repository = mock(EmailDeliveryRepository.class);
        EmailService email = mock(EmailService.class);
        when(repository.findById(any())).thenReturn(Optional.of(new EmailDelivery("hash", "DELIVERED")));
        String message = new Gson().toJson(EmailDetails.builder().recipient("user@example.test").build());
        new EmailDeliveryService(repository, email).deliverOnboardingEvent(message);
        verifyNoInteractions(email);
        verify(repository, never()).save(any());
    }
}
