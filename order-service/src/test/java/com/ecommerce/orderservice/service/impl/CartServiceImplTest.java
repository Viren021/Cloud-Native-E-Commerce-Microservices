package com.ecommerce.orderservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.orderservice.dto.order.CartDto;
import com.ecommerce.orderservice.dto.user.UserDto;
import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.repository.CartRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CallAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock CartRepository cartRepository;
    @Mock OrderRepository orderRepository;
    @Mock CallAPI callAPI;
    CartServiceImpl service;

    @BeforeEach void setUp() { service = new CartServiceImpl(cartRepository, orderRepository, callAPI); }

    @Test void userCannotReadUpdateOrDeleteAnotherUsersCart() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(cartRepository.findByCartIdAndUserId(200, 10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.findById(200, "Bearer A", false));
        assertThrows(BusinessException.class, () -> service.update(200, CartDto.builder().userId(10L).build(), "Bearer A", false));
        assertThrows(BusinessException.class, () -> service.deleteById(200, "Bearer A", false));
        verify(cartRepository, times(3)).findByCartIdAndUserId(200, 10L);
        verify(cartRepository, never()).findById(200);
        verify(cartRepository, never()).delete(any(Cart.class));
    }

    @Test void forgedUserIdOnCartCreationIsIgnoredAndOwnerIsResolvedFromAuth() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CartDto saved = service.save(CartDto.builder().userId(99L).cartId(200).build(), "Bearer A");
        assertEquals(10L, saved.getUserId());
        verify(cartRepository).save(argThat(cart -> cart.getUserId().equals(10L) && cart.getCartId() == null));
    }

    @Test void ownerCanReadOwnCart() {
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(cartRepository.findByCartIdAndUserId(100, 10L)).thenReturn(Optional.of(Cart.builder().cartId(100).userId(10L).build()));
        assertEquals(100, service.findById(100, "Bearer A", false).getCartId());
    }

    @Test void ownerCanUpdateOwnCartWithoutChangingItsOwner() {
        Cart cart = Cart.builder().cartId(100).userId(10L).build();
        when(callAPI.receiverCurrentUserDto("Bearer A")).thenReturn(UserDto.builder().id(10L).build());
        when(cartRepository.findByCartIdAndUserId(100, 10L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        CartDto result = service.update(100, CartDto.builder().cartId(100).userId(99L).build(), "Bearer A", false);
        assertEquals(10L, result.getUserId());
    }

    @Test void administratorCanReadAnyCart() {
        when(cartRepository.findById(200)).thenReturn(Optional.of(Cart.builder().cartId(200).userId(99L).build()));
        assertEquals(200, service.findById(200, "Bearer ADMIN", true).getCartId());
        verify(callAPI, never()).receiverCurrentUserDto(any());
    }
}
