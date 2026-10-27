package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    List<Cart> findAllByUserId(Long userId);
    Page<Cart> findAllByUserId(Long userId, Pageable pageable);
    Optional<Cart> findByCartIdAndUserId(Integer cartId, Long userId);
}
