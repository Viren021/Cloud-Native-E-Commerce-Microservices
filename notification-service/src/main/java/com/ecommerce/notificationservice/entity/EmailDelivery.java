package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class EmailDelivery {
    @Id
    private String eventKey;
    private String status;
    private LocalDateTime updatedAt;

    public EmailDelivery(String eventKey, String status) {
        this.eventKey = eventKey;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
