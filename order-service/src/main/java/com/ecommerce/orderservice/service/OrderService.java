package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.order.OrderDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    List<OrderDto> findAll(String authorization, boolean administrator);

    Page<OrderDto> findAll(int page, int size, String sortBy, String sortOrder, String authorization, boolean administrator);

    OrderDto findById(Integer orderId, String authorization, boolean administrator);

    OrderDto save(OrderDto orderDto, String authorization);

    OrderDto update(OrderDto orderDto);

    OrderDto update(Integer orderId, OrderDto orderDto, String authorization, boolean administrator);

    void deleteById(Integer orderId, String authorization, boolean administrator);

    Boolean existsByOrderId(Integer orderId, String authorization, boolean administrator);
}
