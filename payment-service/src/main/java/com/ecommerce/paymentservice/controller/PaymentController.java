package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.PaymentDto;
import com.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.ecommerce.paymentservice.security.JwtTokenFilter;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PaymentDto>> findAll() {
        log.info("PaymentDto List, controller; fetch all payments");
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<PaymentDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        return ResponseEntity.ok(paymentService.findAll(page, size, sortBy, sortOrder));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<PaymentDto> findById(
            @PathVariable("paymentId")
            @NotBlank(message = "Input must not be blank")
            @Valid final String paymentId) {
        log.info("PaymentDto, resource; fetch payment by id");
        return ResponseEntity.ok(paymentService.findById(Integer.parseInt(paymentId), JwtTokenFilter.getTokenFromRequest(), isAdministrator()));
    }

    @GetMapping("/getOrder/{orderId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<OrderDto> getOrderDto(@PathVariable("orderId") final Integer orderId) {
        return ResponseEntity.ok(paymentService.getOrderDto(orderId, JwtTokenFilter.getTokenFromRequest()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<PaymentDto> save(
            @RequestBody
            @NotNull(message = "Input must not be NULL!")
            @Valid final PaymentDto paymentDto) {
        log.info("PaymentDto, resource; save payment");
        return ResponseEntity.ok(paymentService.save(paymentDto, JwtTokenFilter.getTokenFromRequest()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentDto> update(
            @RequestBody
            @NotNull(message = "Input must not be NULL")
            @Valid final PaymentDto paymentDto) {
        log.info("PaymentDto, resource; update payment");
        return ResponseEntity.ok(paymentService.update(paymentDto));
    }

    @PutMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentDto> update(
            @PathVariable("paymentId")
            @NotNull(message = "Input must not be null")
            @Valid final Integer paymentId,
            @RequestBody
            @NotNull(message = "Input must not be NULL")
            @Valid final PaymentDto paymentDto) {
        log.info("PaymentDto, resource; update payment with paymentId");
        return ResponseEntity.ok(paymentService.update(paymentId, paymentDto));
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<Boolean> deleteById(@PathVariable("paymentId") final Integer paymentId) {
        log.info("Boolean, resource; delete payment by id");
        paymentService.deleteById(paymentId, JwtTokenFilter.getTokenFromRequest(), isAdministrator());
        return ResponseEntity.ok(true);
    }

    private boolean isAdministrator() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority()));
    }
}
