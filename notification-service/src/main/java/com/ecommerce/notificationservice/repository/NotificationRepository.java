package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByRecipientId(String recipientId);
    Optional<Notification> findByIdAndRecipientId(Long id, String recipientId);

}
