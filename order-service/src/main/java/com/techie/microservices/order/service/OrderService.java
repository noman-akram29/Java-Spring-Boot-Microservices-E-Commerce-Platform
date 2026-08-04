package com.techie.microservices.order.service;

import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;
	private final InventoryClient inventoryClient;
	private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

	public OrderResponse placeOrder(OrderRequest orderRequest) {

		try {
			// 1. CHECK INVENTORY
			boolean isProductInStock = inventoryClient.isInStock(
			                               orderRequest.skuCode(),
			                               orderRequest.quantity()
			                           );

			if (!isProductInStock) {
				log.warn("Out of stock: {}", orderRequest.skuCode());
				return new OrderResponse(
				           null,
				           "FAILED",
				           "Product is out of stock"
				       );
			}

			// 2. DECREASE INVENTORY
			ResponseEntity<InventoryResponse> response =
			    inventoryClient.decreaseInventory(
			        new InventoryRequest(
			            orderRequest.skuCode(),
			            orderRequest.quantity()
			        )
			    );

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				log.error("Inventory decrease failed for SKU: {}", orderRequest.skuCode());
				return new OrderResponse(
				           null,
				           "FAILED",
				           "Inventory update failed"
				       );
			}

			log.info("Inventory updated for SKU: {}", orderRequest.skuCode());

			// 3. SAVE ORDER
			Order order = new Order();
			order.setOrderNumber(UUID.randomUUID().toString());
			order.setPrice(orderRequest.price());
			order.setQuantity(orderRequest.quantity());
			order.setSkuCode(orderRequest.skuCode());

			orderRepository.save(order);

			log.info("Order saved successfully: {}", order.getOrderNumber());

			// 4. SEND EVENT
			OrderPlacedEvent event = new OrderPlacedEvent(
			    order.getOrderNumber(),
			    orderRequest.userDetails().email()
			);

			kafkaTemplate.send("order-placed", event);

			log.info("Kafka event sent: {}", event);

			return new OrderResponse(
			           order.getOrderNumber(),
			           "SUCCESS",
			           "Order placed successfully"
			       );

		} catch (Exception e) {
			log.error("Order processing failed", e);

			return new OrderResponse(
			           null,
			           "FAILED",
			           "Unexpected error: " + e.getMessage()
			       );
		}
	}
}