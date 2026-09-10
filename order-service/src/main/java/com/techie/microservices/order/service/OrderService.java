package com.techie.microservices.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.model.OutboxEvent;
import com.techie.microservices.order.repository.OrderRepository;
import com.techie.microservices.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryClient inventoryClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse placeOrder(OrderRequest orderRequest) {

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

        log.info("Order and Outbox saved: {}", order.getOrderNumber());

        return new OrderResponse(
                order.getOrderNumber(),
                "SUCCESS",
                "Order placed successfully"
        );
    }
}
