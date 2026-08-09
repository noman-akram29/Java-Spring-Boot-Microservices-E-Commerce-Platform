package com.techie.microservices.order.client;

import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url}",
	fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

	@GetMapping
	boolean isInStock(
		@RequestParam String skuCode,
		@RequestParam Integer quantity
	);

	@PostMapping("/decrease")
	ResponseEntity<InventoryResponse> decreaseInventory(
		@RequestBody InventoryRequest request
	);
}