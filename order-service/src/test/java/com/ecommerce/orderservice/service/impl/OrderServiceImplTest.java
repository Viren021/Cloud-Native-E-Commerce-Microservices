package com.ecommerce.orderservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.orderservice.dto.order.CartDto;
import com.ecommerce.orderservice.dto.order.OrderDto;
import com.ecommerce.orderservice.dto.product.ProductDto;
import com.ecommerce.orderservice.dto.user.UserDto;
import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.repository.CartRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CallAPI;
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
class OrderServiceImplTest {
    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock CallAPI callAPI;
    OrderServiceImpl service;

    @BeforeEach void setUp() { service = new OrderServiceImpl(orderRepository, cartRepository, new ModelMapper(), callAPI); }

    @Test void userCannotReadOrCancelAnotherUsersOrder() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(orderRepository.findByOrderIdAndCart_UserId(300, 10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.findById(300, "Bearer A", false));
        assertThrows(BusinessException.class, () -> service.deleteById(300, "Bearer A", false));
        verify(orderRepository, times(2)).findByOrderIdAndCart_UserId(300, 10L);
        verify(orderRepository, never()).findById(300);
        verify(orderRepository, never()).delete(any(Order.class));
    }

    @Test void forgedOwnerAndPriceFieldsAreIgnoredWhenCreatingOrder() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(cartRepository.findByCartIdAndUserId(100, 10L)).thenReturn(Optional.of(Cart.builder().cartId(100).userId(10L).build()));
        when(callAPI.receiverProductDto(7)).thenReturn(ProductDto.builder().productId(7).priceUnit(25.0).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderDto saved = service.save(OrderDto.builder().orderId(991).orderFee(0.01).productId(7)
                .cartDto(CartDto.builder().cartId(100).userId(99L).build()).build(), "Bearer A");
        assertEquals(25.0, saved.getOrderFee());
        assertEquals(10L, saved.getCartDto().getUserId());
        assertNotEquals(991, saved.getOrderId());
        verify(cartRepository).findByCartIdAndUserId(100, 10L);
    }

    @Test void customerUpdatePreservesServerManagedOrderFields() {
        Cart cart = Cart.builder().cartId(100).userId(10L).build();
        Order existing = Order.builder().orderId(300).orderDate(java.time.LocalDateTime.now())
                .orderFee(50.0).productId(7).cart(cart).build();
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(orderRepository.findByOrderIdAndCart_UserId(300, 10L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);
        OrderDto request = OrderDto.builder().orderId(99).orderDesc("note").orderFee(0.01).productId(999)
                .cartDto(CartDto.builder().cartId(200).userId(99L).build()).build();
        OrderDto result = service.update(300, request, "Bearer A", false);
        assertEquals("note", result.getOrderDesc());
        assertEquals(50.0, result.getOrderFee());
        assertEquals(7, result.getProductId());
        assertEquals(100, result.getCartDto().getCartId());
    }

    @Test void ownerCanReadOwnOrder() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(orderRepository.findByOrderIdAndCart_UserId(300, 10L)).thenReturn(Optional.of(
                Order.builder().orderId(300).cart(Cart.builder().cartId(100).userId(10L).build()).build()));
        assertEquals(300, service.findById(300, "Bearer A", false).getOrderId());
    }

    @Test void administratorCanReadAnotherUsersOrder() {
        when(orderRepository.findById(300)).thenReturn(Optional.of(
                Order.builder().orderId(300).cart(Cart.builder().cartId(100).userId(99L).build()).build()));
        assertEquals(300, service.findById(300, "Bearer ADMIN", true).getOrderId());
        verify(callAPI, never()).receiverCurrentUserDto(any());
    }
}
