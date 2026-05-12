package com.techie.microservices.order.service;

import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional

public class OrderService {

    // private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public boolean placeOrder(OrderRequest orderRequest) {

        try {
            boolean isProductInStock = inventoryClient.isInStock(
                    orderRequest.skuCode(),
                    orderRequest.quantity()
            );

            if (!isProductInStock) {
                log.warn("Out of stock: {}", orderRequest.skuCode());
                return false;
            }

            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price());
            order.setQuantity(orderRequest.quantity());
            order.setSkuCode(orderRequest.skuCode());

            orderRepository.save(order);

            ResponseEntity<InventoryResponse> response =
                    inventoryClient.decreaseInventory(
                            new InventoryRequest(
                                    orderRequest.skuCode(),
                                    orderRequest.quantity()
                            )
                    );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Inventory update failed");
                return false;
            }

            log.info("Inventory updated for SKU: {}", orderRequest.skuCode());

            OrderPlacedEvent event = new OrderPlacedEvent(
                    order.getOrderNumber(),
                    orderRequest.userDetails().email()
            );

            kafkaTemplate.send("order-placed", event);

            log.info("Kafka event sent: {}", event);

            return true;

        } catch (Exception e) {
            log.error("Order processing failed", e);
            return false;
        }
    }
}
