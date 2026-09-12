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
import org.springframework.transaction.annotation.Propagation;
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

        public OrderResponse placeOrder(String idempotencyKey, OrderRequest orderRequest) {

                // 0. FAST PATH: already fully processed
                Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
                if (existingRecord.isPresent()) {
                        log.info("Idempotency key match found. Returning cached order response for key: {}",
                                        idempotencyKey);
                        return new OrderResponse(
                                        existingRecord.get().getOrderNumber(),
                                        "SUCCESS",
                                        "Order already placed (idempotent replay)");
                }

                // 1. RESERVE the idempotency key ATOMICALLY, in its own committed transaction,
                // BEFORE calling any external service. This is what actually closes the race:
                // only one concurrent caller can win this insert.
                String orderNumber = UUID.randomUUID().toString();
                boolean reserved = tryReserveIdempotencyKey(idempotencyKey, orderNumber);

                if (!reserved) {
                        // Someone else already reserved (or completed) this key while we were racing.
                        IdempotencyRecord winner = idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Idempotency recovery failed after collision"));
                        log.warn("Concurrent idempotency key collision detected for key: {}", idempotencyKey);
                        return new OrderResponse(
                                        winner.getOrderNumber(),
                                        "SUCCESS",
                                        "Order already placed (concurrent replay)");
                }

                // 2. NOW it is safe to call inventory — exactly one caller reaches this point
                // per key.
                ResponseEntity<InventoryResponse> response;
                try {
                        response = inventoryClient.decreaseInventory(
                                        new InventoryRequest(orderRequest.skuCode(), orderRequest.quantity()));
                } catch (Exception e) {
                        log.warn("Inventory reservation failed for SKU {}: {}", orderRequest.skuCode(), e.getMessage());
                        idempotencyRepository.deleteByIdempotencyKey(idempotencyKey); // release reservation, allow
                                                                                      // retry
                        return new OrderResponse(null, "FAILED", "Product is out of stock or inventory update failed");
                }

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        log.warn("Insufficient stock or inventory error for SKU: {}", orderRequest.skuCode());
                        idempotencyRepository.deleteByIdempotencyKey(idempotencyKey);
                        return new OrderResponse(null, "FAILED", "Product is out of stock or inventory update failed");
                }

                // 3. SAVE ORDER + OUTBOX. If this fails, COMPENSATE by restoring inventory.
                try {
                        saveOrderAndOutbox(orderNumber, orderRequest);
                } catch (Exception e) {
                        log.error("Order persistence failed after inventory was decremented for SKU {}. Compensating.",
                                        orderRequest.skuCode(), e);
                        compensateInventory(orderRequest);
                        idempotencyRepository.deleteByIdempotencyKey(idempotencyKey);
                        return new OrderResponse(null, "FAILED",
                                        "Order could not be completed; inventory has been restored");
                }

                log.info("Order, Outbox, and Idempotency saved: {}", orderNumber);
                return new OrderResponse(orderNumber, "SUCCESS", "Order placed successfully");
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        protected boolean tryReserveIdempotencyKey(String idempotencyKey, String orderNumber) {
                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdempotencyKey(idempotencyKey);
                record.setOrderNumber(orderNumber);
                try {
                        idempotencyRepository.save(record);
                        return true;
                } catch (DataIntegrityViolationException e) {
                        return false;
                }
        }

        @Transactional
        protected void saveOrderAndOutbox(String orderNumber, OrderRequest orderRequest) {
                Order order = new Order();
                order.setOrderNumber(orderNumber);
                order.setPrice(orderRequest.price());
                order.setQuantity(orderRequest.quantity());
                order.setSkuCode(orderRequest.skuCode());
                orderRepository.save(order);

                OrderPlacedEvent event = new OrderPlacedEvent(orderNumber, orderRequest.userDetails().email());

                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setAggregateType("Order");
                outboxEvent.setAggregateId(orderNumber);
                outboxEvent.setEventType("OrderPlaced");

                try {
                        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Failed to serialize OrderPlacedEvent", e);
                }

                outboxRepository.save(outboxEvent);
        }

        private void compensateInventory(OrderRequest orderRequest) {
                try {
                        inventoryClient.increaseInventory(
                                        new InventoryRequest(orderRequest.skuCode(), orderRequest.quantity()));
                } catch (Exception ex) {
                        log.error("CRITICAL: compensating inventory restore FAILED for SKU {}. Manual reconciliation required.",
                                        orderRequest.skuCode(), ex);
                }
        }
}