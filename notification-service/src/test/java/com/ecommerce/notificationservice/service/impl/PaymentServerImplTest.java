package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.dto.PaymentDto;
import com.ecommerce.notificationservice.entity.Payment;
import com.ecommerce.notificationservice.entity.PaymentStatus;
import com.ecommerce.notificationservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentServerImplTest {
    @Test void replayedPaymentEventUpdatesExistingPaymentInsteadOfCreatingDuplicate() {
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment existing = Payment.builder().id(8L).paymentId(21).userId(77L).orderId(30).isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS).build();
        when(repository.findByPaymentId(21)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        PaymentDto event = PaymentDto.builder().paymentId(21).userId(77L).orderId(30).isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED).build();
        Payment result = new PaymentServerImpl(repository).savePayment(event);
        assertEquals(8L, result.getId());
        assertEquals(PaymentStatus.COMPLETED, existing.getPaymentStatus());
        verify(repository, never()).save(argThat(payment -> payment != existing));
    }
}
