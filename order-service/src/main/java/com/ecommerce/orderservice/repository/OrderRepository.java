package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findAllByCart_UserId(Long userId);
    Page<Order> findAllByCart_UserId(Long userId, Pageable pageable);
    Optional<Order> findByOrderIdAndCart_UserId(Integer orderId, Long userId);
    boolean existsByOrderIdAndCart_UserId(Integer orderId, Long userId);
    List<Order> findAllByOrderIdInAndCart_UserId(Collection<Integer> orderIds, Long userId);
    @Modifying
    @Query("DELETE FROM Order o WHERE o.cart = :cart")
    void deleteAllByCart(Cart cart);
}
