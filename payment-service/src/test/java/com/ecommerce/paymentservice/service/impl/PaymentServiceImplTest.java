package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.PaymentDto;
import com.ecommerce.paymentservice.dto.UserDto;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.CallAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock PaymentRepository paymentRepository;
    @Mock CallAPI callAPI;
    PaymentServiceImpl service;

    @BeforeEach void setUp() { service = new PaymentServiceImpl(paymentRepository, new ModelMapper(), callAPI); }

    @Test void userCannotAccessAnotherUsersPayment() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(paymentRepository.findByPaymentIdAndUserId(500, 10L)).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.findById(500, "Bearer A", false));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, error.getStatus());
        verify(callAPI, never()).receiverPaymentDto(any(), any());
    }

    @Test void forgedOwnerAndSuccessfulStatusAreIgnoredOnPaymentCreation() {
        when(callAPI.receiverPaymentDto(300, "Bearer A")).thenReturn(OrderDto.builder().orderId(300).build());
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(paymentRepository.existsByOrderIdAndIsPayed(300)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0); p.setPaymentId(500); return p;
        });
        PaymentDto result = service.save(PaymentDto.builder().paymentId(1).orderId(300).userId(99L)
                .isPayed(true).paymentStatus(PaymentStatus.COMPLETED).build(), "Bearer A");
        assertEquals(10L, result.getUserId());
        assertEquals(false, result.getIsPayed());
        assertEquals(PaymentStatus.NOT_STARTED, result.getPaymentStatus());
        verify(paymentRepository).save(argThat(p -> p.getUserId().equals(10L) && !p.getIsPayed()
                && p.getPaymentStatus() == PaymentStatus.NOT_STARTED));
    }

    @Test void storedPaymentOwnerMustAlsoOwnRelatedOrder() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(paymentRepository.findByPaymentIdAndUserId(500, 10L)).thenReturn(Optional.of(
                Payment.builder().paymentId(500).userId(10L).orderId(300).build()));
        when(callAPI.receiverPaymentDto(300, "Bearer A"))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));
        BusinessException error = assertThrows(BusinessException.class, () -> service.findById(500, "Bearer A", false));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test void administratorCanReadAnyPaymentById() {
        Payment payment = Payment.builder().paymentId(500).userId(99L).orderId(300).build();
        when(paymentRepository.findById(500)).thenReturn(Optional.of(payment));
        assertEquals(500, service.findById(500, "Bearer ADMIN", true).getPaymentId());
        verify(callAPI, never()).receiverCurrentUserDto(any());
    }

    @Test void ownerCanReadOwnPaymentAndItsOrder() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(paymentRepository.findByPaymentIdAndUserId(500, 10L)).thenReturn(Optional.of(
                Payment.builder().paymentId(500).userId(10L).orderId(300).isPayed(false).build()));
        when(callAPI.receiverPaymentDto(300, "Bearer A")).thenReturn(OrderDto.builder().orderId(300).build());
        PaymentDto result = service.findById(500, "Bearer A", false);
        assertEquals(500, result.getPaymentId());
        verify(callAPI).receiverPaymentDto(300, "Bearer A");
    }
}
