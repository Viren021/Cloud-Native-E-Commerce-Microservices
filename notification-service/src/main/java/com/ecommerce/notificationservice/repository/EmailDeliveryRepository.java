package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.EmailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, String> {
}
