package com.techie.microservices.order.repository;

import com.techie.microservices.order.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Derived delete queries must run inside a transaction.
     * Called from OrderService when compensating after inventory failure.
     */
    @Transactional
    void deleteByIdempotencyKey(String idempotencyKey);
}