package com.ecommerce.orderservice.service.impl;

import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.orderservice.dto.order.CartDto;
import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.helper.CartMappingHelper;
import com.ecommerce.orderservice.repository.CartRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CallAPI;
import com.ecommerce.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CallAPI callAPI;

    private Long ownerId(String authorization, boolean administrator) {
        if (administrator) return null;
        return callAPI.receiverCurrentUserDto(authorization).getId();
    }

    @Override
    public List<CartDto> findAll(String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        List<Cart> carts = administrator ? cartRepository.findAll() : cartRepository.findAllByUserId(owner);
        return carts.stream().map(CartMappingHelper::map).toList();
    }

    @Override
    public Page<CartDto> findAll(int page, int size, String sortBy, String sortOrder, String authorization, boolean administrator) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortOrder), sortBy));
        Long owner = ownerId(authorization, administrator);
        Page<Cart> carts = administrator ? cartRepository.findAll(pageable) : cartRepository.findAllByUserId(owner, pageable);
        return carts.map(CartMappingHelper::map);
    }

    @Override
    public CartDto findById(Integer cartId, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        Cart cart = (administrator ? cartRepository.findById(cartId) : cartRepository.findByCartIdAndUserId(cartId, owner))
                .orElseThrow(() -> BusinessException.notFound("cart.not.found"));
        return CartMappingHelper.map(cart);
    }

    @Override
    public CartDto save(CartDto cartDto, String authorization) {
        Long owner = callAPI.receiverCurrentUserDto(authorization).getId();
        Cart cart = Cart.builder().userId(owner).orders(Set.of()).build();
        return CartMappingHelper.map(cartRepository.save(cart));
    }

    @Override
    public CartDto update(CartDto cartDto) {
        Cart existing = cartRepository.findById(cartDto.getCartId())
                .orElseThrow(() -> BusinessException.notFound("cart.not.found"));
        Cart replacement = CartMappingHelper.map(cartDto);
        replacement.setCartId(existing.getCartId());
        return CartMappingHelper.map(cartRepository.save(replacement));
    }

    @Override
    public CartDto update(Integer cartId, CartDto cartDto, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        Cart existing = (administrator ? cartRepository.findById(cartId) : cartRepository.findByCartIdAndUserId(cartId, owner))
                .orElseThrow(() -> BusinessException.notFound("cart.not.found"));
        if (!administrator && cartDto.getOrderDtos() != null) {
            List<Integer> requestedIds = cartDto.getOrderDtos().stream().map(dto -> dto.getOrderId()).toList();
            if (requestedIds.stream().anyMatch(Objects::isNull)) throw BusinessException.badRequest("error.bad.request");
            List<Order> ownedOrders = requestedIds.isEmpty() ? List.of()
                    : orderRepository.findAllByOrderIdInAndCart_UserId(requestedIds, owner);
            if (ownedOrders.size() != requestedIds.stream().distinct().count()) {
                throw BusinessException.notFound("order.not.found");
            }
            ownedOrders.forEach(order -> order.setCart(existing));
            existing.setOrders(new HashSet<>(ownedOrders));
        }
        // Caller supplied cartId/userId are never applied; nested order fields other than owned IDs are ignored.
        return CartMappingHelper.map(cartRepository.save(existing));
    }

    @Override
    public void deleteById(Integer cartId, String authorization, boolean administrator) {
        Long owner = ownerId(authorization, administrator);
        Cart cart = (administrator ? cartRepository.findById(cartId) : cartRepository.findByCartIdAndUserId(cartId, owner))
                .orElseThrow(() -> BusinessException.notFound("cart.not.found"));
        orderRepository.deleteAllByCart(cart);
        cartRepository.delete(cart);
    }
}
