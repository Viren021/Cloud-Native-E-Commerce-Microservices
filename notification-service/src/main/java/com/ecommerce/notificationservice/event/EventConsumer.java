package com.ecommerce.notificationservice.event;

import com.ecommerce.notificationservice.constant.KafkaConstant;
import com.ecommerce.notificationservice.dto.PaymentDto;
import com.ecommerce.notificationservice.service.PaymentService;
import com.ecommerce.notificationservice.service.impl.EmailDeliveryService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final EmailDeliveryService emailDeliveryService;
    private final PaymentService paymentService;
    private final EventProducer eventProducer;
    private final Gson gson = new Gson();

    @KafkaListener(topics = KafkaConstant.PROFILE_ONBOARDING_TOPIC,
            groupId = "${notification.kafka.consumer-group-id}")
    public void sendEmailKafkaOnboarding(String message) {
        log.info("AUTH-SERVICE Onboarding event: send email on notification service");
        String result = emailDeliveryService.deliverOnboardingEvent(message);
        log.info("Email sent: {}", result);
        eventProducer.send(KafkaConstant.PROFILE_ONBOARDED_TOPIC, gson.toJson(result));
    }

    @KafkaListener(topics = KafkaConstant.STATUS_PAYMENT_SUCCESSFUL,
            groupId = "${notification.kafka.consumer-group-id}")
    public void paymentOrderKafkaOnboarding(String message) {
        log.info("Payment event received on notification-service");
        PaymentDto paymentDto = gson.fromJson(message, PaymentDto.class);
        if (paymentDto == null || paymentDto.getPaymentId() == null) {
            log.warn("Ignoring payment event without a stable payment id");
            return;
        }
        paymentService.savePayment(paymentDto);
        eventProducer.send(KafkaConstant.PROFILE_ONBOARDED_TOPIC, gson.toJson(paymentDto));

        // Payment events do not currently carry a trusted recipient email address.
        // Do not send customer payment notices to a hard-coded or caller-controlled address.
    }

}
