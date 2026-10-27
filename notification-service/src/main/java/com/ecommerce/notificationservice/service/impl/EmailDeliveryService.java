package com.ecommerce.notificationservice.service.impl;

import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.entity.EmailDelivery;
import com.ecommerce.notificationservice.repository.EmailDeliveryRepository;
import com.ecommerce.notificationservice.service.EmailService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailDeliveryService {
    private final EmailDeliveryRepository repository;
    private final EmailService emailService;
    private final Gson gson = new Gson();

    public String deliverOnboardingEvent(String message) {
        String key = sha256(message);
        EmailDelivery delivery = repository.findById(key).orElse(null);
        if (delivery != null && !"FAILED".equals(delivery.getStatus())) return "Duplicate event ignored";
        if (delivery == null) {
            delivery = new EmailDelivery(key, "PENDING");
            try {
                repository.saveAndFlush(delivery);
            } catch (DataIntegrityViolationException duplicate) {
                return "Duplicate event ignored";
            }
        } else {
            delivery.setStatus("PENDING");
            delivery.setUpdatedAt(LocalDateTime.now());
            repository.saveAndFlush(delivery);
        }

        String result = emailService.sendSimpleMail(gson.fromJson(message, EmailDetails.class));
        delivery.setStatus(result != null && result.toLowerCase(Locale.ROOT).contains("sent successfully") ? "DELIVERED" : "FAILED");
        delivery.setUpdatedAt(LocalDateTime.now());
        repository.save(delivery);
        return result;
    }

    private String sha256(String message) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
