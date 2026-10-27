package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.order.CartDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface CartService {
    List<CartDto> findAll(String authorization, boolean administrator);
    Page<CartDto> findAll(int page, int size, String sortBy, String sortOrder, String authorization, boolean administrator);
    CartDto findById(Integer cartId, String authorization, boolean administrator);
    CartDto save(CartDto cartDto, String authorization);
    CartDto update(CartDto cartDto);
    CartDto update(Integer cartId, CartDto cartDto, String authorization, boolean administrator);
    void deleteById(Integer cartId, String authorization, boolean administrator);
}
