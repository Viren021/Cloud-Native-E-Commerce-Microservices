package com.ecommerce.notificationservice.event;

import com.ecommerce.notificationservice.dto.PaymentDto;
import com.ecommerce.notificationservice.entity.PaymentStatus;
import com.ecommerce.notificationservice.service.PaymentService;
import com.ecommerce.notificationservice.service.impl.EmailDeliveryService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventConsumerTest {
    @Test void trustedOnboardingKafkaEventStillDeliversRegistrationEmail() {
        EmailDeliveryService emailDelivery = mock(EmailDeliveryService.class);
        PaymentService payments = mock(PaymentService.class);
        EventProducer producer = mock(EventProducer.class);
        EventConsumer consumer = new EventConsumer(emailDelivery, payments, producer);
        String event = new Gson().toJson(com.ecommerce.notificationservice.dto.EmailDetails.builder()
                .recipient("new-user@example.test").subject("Welcome").msgBody("welcome").build());
        consumer.sendEmailKafkaOnboarding(event);
        verify(emailDelivery).deliverOnboardingEvent(event);
    }

    @Test void paymentEventDoesNotSendToHardCodedOrPayloadControlledAddress() {
        EmailDeliveryService emailDelivery = mock(EmailDeliveryService.class);
        PaymentService payments = mock(PaymentService.class);
        EventProducer producer = mock(EventProducer.class);
        EventConsumer consumer = new EventConsumer(emailDelivery, payments, producer);
        PaymentDto dto = PaymentDto.builder().paymentId(21).orderId(30).userId(77L)
                .isPayed(true).paymentStatus(PaymentStatus.COMPLETED).build();
        consumer.paymentOrderKafkaOnboarding(new Gson().toJson(dto));
        verify(payments).savePayment(any());
        verifyNoInteractions(emailDelivery);
    }
}
