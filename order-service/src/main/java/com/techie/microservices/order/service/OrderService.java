package com.techie.microservices.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.IdempotencyRecord;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.model.OutboxEvent;
import com.techie.microservices.order.repository.IdempotencyRepository;
import com.techie.microservices.order.repository.OrderRepository;
import com.techie.microservices.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final InventoryClient inventoryClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse placeOrder(String idempotencyKey, OrderRequest orderRequest) {

        // 0. CHECK IDEMPOTENCY (Replay protection)
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            log.info("Idempotency key match found. Returning cached order response for key: {}", idempotencyKey);
            return new OrderResponse(
                    existingRecord.get().getOrderNumber(),
                    "SUCCESS",
                    "Order already placed (idempotent replay)"
            );
        }

        // 1. ATOMICALLY RESERVE/DECREASE INVENTORY
        ResponseEntity<InventoryResponse> response;
        try {
            response = inventoryClient.decreaseInventory(
                    new InventoryRequest(
                            orderRequest.skuCode(),
                            orderRequest.quantity()
                    )
            );
        } catch (Exception e) {
            log.warn(
                    "Inventory reservation failed for SKU {}: {}",
                    orderRequest.skuCode(),
                    e.getMessage()
            );
            return new OrderResponse(
                    null,
                    "FAILED",
                    "Product is out of stock or inventory update failed"
            );
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn(
                    "Insufficient stock or inventory error for SKU: {}",
                    orderRequest.skuCode()
            );
            return new OrderResponse(
                    null,
                    "FAILED",
                    "Product is out of stock or inventory update failed"
            );
        }

        // 2. SAVE ORDER + OUTBOX in the SAME transaction
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setPrice(orderRequest.price());
        order.setQuantity(orderRequest.quantity());
        order.setSkuCode(orderRequest.skuCode());
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getOrderNumber(),
                orderRequest.userDetails().email()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("Order");
        outboxEvent.setAggregateId(order.getOrderNumber());
        outboxEvent.setEventType("OrderPlaced");

        try {
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OrderPlacedEvent", e);
        }

        outboxRepository.save(outboxEvent);

        // 3. SAVE IDEMPOTENCY RECORD (With concurrent race condition safety)
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setOrderNumber(order.getOrderNumber());

        try {
            idempotencyRepository.save(idempotencyRecord);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent idempotency key collision detected for key: {}", idempotencyKey);
            IdempotencyRecord winnerRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency recovery failed after collision"));
            return new OrderResponse(
                    winnerRecord.getOrderNumber(),
                    "SUCCESS",
                    "Order already placed (concurrent replay)"
            );
        }

        log.info("Order, Outbox, and Idempotency saved: {}", order.getOrderNumber());

        return new OrderResponse(
                order.getOrderNumber(),
                "SUCCESS",
                "Order placed successfully"
        );
    }
}