package com.ecommerce.orderservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.orderservice.dto.order.OrderDto;
import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.helper.OrderMappingHelper;
import com.ecommerce.orderservice.repository.CartRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CallAPI;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;
    private final CallAPI callAPI;

    private Long ownerId(String authorization, boolean administrator) {
        return administrator ? null : callAPI.receiverCurrentUserDto(authorization).getId();
    }

    private List<OrderDto> mapOrders(List<Order> orders) {
        return orders.stream().map(OrderMappingHelper::map).map(this::attachProduct).toList();
    }

    private OrderDto attachProduct(OrderDto dto) {
        try { dto.setProductDto(callAPI.receiverProductDto(dto.getProductId())); }
        catch (Exception ignored) { /* Product enrichment is optional for reads. */ }
        return dto;
    }

    @Override
    public List<OrderDto> findAll(String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        return mapOrders(administrator ? orderRepository.findAll() : orderRepository.findAllByCart_UserId(owner));
    }

    @Override
    public Page<OrderDto> findAll(int page, int size, String sortBy, String sortOrder, String authorization, boolean administrator) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortOrder), sortBy));
        Long owner = ownerId(authorization, administrator);
        Page<Order> orders = administrator ? orderRepository.findAll(pageable) : orderRepository.findAllByCart_UserId(owner, pageable);
        return orders.map(OrderMappingHelper::map).map(this::attachProduct);
    }

    @Override
    public OrderDto findById(Integer orderId, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        Order order = (administrator ? orderRepository.findById(orderId) : orderRepository.findByOrderIdAndCart_UserId(orderId, owner))
                .orElseThrow(() -> BusinessException.notFound("order.not.found"));
        return attachProduct(OrderMappingHelper.map(order));
    }

    @Override
    public Boolean existsByOrderId(Integer orderId, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        return administrator ? orderRepository.existsById(orderId) : orderRepository.existsByOrderIdAndCart_UserId(orderId, owner);
    }

    @Override
    public OrderDto save(OrderDto orderDto, String authorization) {
        Long owner = callAPI.receiverCurrentUserDto(authorization).getId();
        if (orderDto.getCartDto() == null || orderDto.getCartDto().getCartId() == null || orderDto.getProductId() == null) {
            throw BusinessException.badRequest("error.bad.request");
        }
        Cart cart = cartRepository.findByCartIdAndUserId(orderDto.getCartDto().getCartId(), owner)
                .orElseThrow(() -> BusinessException.notFound("cart.not.found"));
        var product = callAPI.receiverProductDto(orderDto.getProductId());
        if (product == null || product.getPriceUnit() == null) {
            throw BusinessException.notFound("product.not.found");
        }
        Order order = Order.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc(orderDto.getOrderDesc())
                .orderFee(product.getPriceUnit())
                .productId(product.getProductId() == null ? orderDto.getProductId() : product.getProductId())
                .cart(cart)
                .build();
        return OrderMappingHelper.map(orderRepository.save(order));
    }

    @Override
    public OrderDto update(OrderDto orderDto) {
        return OrderMappingHelper.map(orderRepository.save(OrderMappingHelper.map(orderDto)));
    }

    @Override
    public OrderDto update(Integer orderId, OrderDto orderDto, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        if (administrator) {
            orderDto.setOrderId(orderId);
            return update(orderDto);
        }
        Order existing = orderRepository.findByOrderIdAndCart_UserId(orderId, owner)
                .orElseThrow(() -> BusinessException.notFound("order.not.found"));
        // Customers may update only the descriptive note. Price, product, date and cart are server-managed.
        if (orderDto.getOrderDesc() != null) existing.setOrderDesc(orderDto.getOrderDesc());
        return OrderMappingHelper.map(orderRepository.save(existing));
    }

    @Override
    public void deleteById(Integer orderId, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        Order order = (administrator ? orderRepository.findById(orderId) : orderRepository.findByOrderIdAndCart_UserId(orderId, owner))
                .orElseThrow(() -> BusinessException.notFound("order.not.found"));
        orderRepository.delete(order);
    }
}
