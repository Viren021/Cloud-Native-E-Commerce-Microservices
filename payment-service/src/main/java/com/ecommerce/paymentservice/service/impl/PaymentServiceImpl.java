package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.commonlib.exception.ErrorCode;
import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.PaymentDto;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.helper.PaymentMappingHelper;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.CallAPI;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final CallAPI callAPI;

    private record PaymentAccess(Payment payment, OrderDto order) { }

    private PaymentAccess getPayment(Integer paymentId, String authorization, boolean administrator) {
        if (administrator) {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> BusinessException.notFound("payment.not.found"));
            return new PaymentAccess(payment, null);
        }
        Long ownerId = callAPI.receiverCurrentUserDto(authorization).getId();
        Payment payment = paymentRepository.findByPaymentIdAndUserId(paymentId, ownerId)
                .orElseThrow(() -> BusinessException.notFound("payment.not.found"));
        // The order service enforces ownership through order -> cart -> local user, guarding against stale/forged payment.userId.
        return new PaymentAccess(payment, orderForCaller(payment.getOrderId(), authorization));
    }

    private OrderDto orderForCaller(Integer orderId, String authorization) {
        try {
            return callAPI.receiverPaymentDto(orderId, authorization);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 403) {
                throw BusinessException.notFound("payment.not.found", ex);
            }
            throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE);
        } catch (RuntimeException ex) {
            throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public List<PaymentDto> findAll() {
        return paymentRepository.findAll().stream().map(PaymentMappingHelper::map).toList();
    }

    @Override
    public Page<PaymentDto> findAll(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortOrder), sortBy));
        return paymentRepository.findAll(pageable).map(PaymentMappingHelper::map);
    }

    @Override
    public PaymentDto findById(Integer paymentId, String authorization, boolean administrator) {
        PaymentAccess access = getPayment(paymentId, authorization, administrator);
        Payment payment = access.payment();
        PaymentDto dto = PaymentMappingHelper.map(payment);
        if (administrator) {
            try { dto.setOrderDto(callAPI.receiverPaymentDto(payment.getOrderId(), authorization)); }
            catch (RuntimeException ex) { throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE); }
        } else dto.setOrderDto(access.order());
        return dto;
    }

    @Override
    public OrderDto getOrderDto(Integer orderId, String authorization) {
        return orderForCaller(orderId, authorization);
    }

    @Override
    public PaymentDto save(PaymentDto paymentDto, String authorization) {
        if (paymentDto.getOrderId() == null) throw BusinessException.badRequest("error.bad.request");
        OrderDto ownedOrder = orderForCaller(paymentDto.getOrderId(), authorization);
        if (ownedOrder == null) throw BusinessException.notFound("payment.not.found");
        Long ownerId = callAPI.receiverCurrentUserDto(authorization).getId();
        if (paymentRepository.existsByOrderIdAndIsPayed(paymentDto.getOrderId())) {
            throw BusinessException.conflict("payment.failed");
        }
        Payment payment = Payment.builder()
                .orderId(paymentDto.getOrderId())
                .userId(ownerId)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();
        return PaymentMappingHelper.map(paymentRepository.save(payment));
    }

    @Override
    public PaymentDto update(PaymentDto paymentDto) {
        return PaymentMappingHelper.map(paymentRepository.save(PaymentMappingHelper.map(paymentDto)));
    }

    @Override
    public PaymentDto update(Integer paymentId, PaymentDto paymentDto) {
        Payment existing = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.notFound("payment.not.found"));
        Payment incoming = PaymentMappingHelper.map(paymentDto);
        existing.setOrderId(incoming.getOrderId());
        existing.setUserId(incoming.getUserId());
        existing.setIsPayed(incoming.getIsPayed());
        existing.setPaymentStatus(incoming.getPaymentStatus());
        return PaymentMappingHelper.map(paymentRepository.save(existing));
    }

    @Override
    public void deleteById(Integer paymentId, String authorization, boolean administrator) {
        paymentRepository.delete(getPayment(paymentId, authorization, administrator).payment());
    }
}
