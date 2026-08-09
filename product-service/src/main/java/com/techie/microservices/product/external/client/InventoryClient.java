package com.techie.microservices.product.external.client;

import com.techie.microservices.product.external.dto.InventoryRequest;
import com.techie.microservices.product.external.dto.InventoryResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url}"
)
public interface InventoryClient {

	@PostMapping
	ResponseEntity<InventoryResponse> upsertInventory(
		@RequestBody InventoryRequest inventoryRequest
	);

	@GetMapping("/{skuCode}")
	ResponseEntity<InventoryResponse> getInventoryBySkuCode(
		@PathVariable String skuCode
	);
}