package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.PaymentDto;
import com.ecommerce.notificationservice.entity.Payment;
import com.ecommerce.notificationservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment-notifications")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Payment> savePayment(@RequestBody PaymentDto paymentDto) {
        return ResponseEntity.ok(paymentService.savePayment(paymentDto));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
