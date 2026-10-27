package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentDto;
import com.ecommerce.paymentservice.dto.OrderDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaymentService {
    List<PaymentDto> findAll();
    Page<PaymentDto> findAll(int page, int size, String sortBy, String sortOrder);
    PaymentDto findById(Integer paymentId, String authorization, boolean administrator);
    OrderDto getOrderDto(Integer orderId, String authorization);
    PaymentDto save(PaymentDto paymentDto, String authorization);
    PaymentDto update(PaymentDto paymentDto);
    PaymentDto update(Integer paymentId, PaymentDto paymentDto);
    void deleteById(Integer paymentId, String authorization, boolean administrator);
}
