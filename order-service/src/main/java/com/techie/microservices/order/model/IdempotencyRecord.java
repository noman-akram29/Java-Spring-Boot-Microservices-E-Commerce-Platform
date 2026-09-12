package com.techie.microservices.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "t_idempotency")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}