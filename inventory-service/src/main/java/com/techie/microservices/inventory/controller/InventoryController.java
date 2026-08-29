package com.techie.microservices.inventory.controller;

import com.techie.microservices.inventory.dto.InventoryRequest;
import com.techie.microservices.inventory.dto.InventoryResponse;
import com.techie.microservices.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Inventory API",
    description = "Manage inventory operations"
)
@RestController
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@Operation(summary = "Check product stock availability")
    @GetMapping
    public boolean isInStock(
        @RequestParam String skuCode,
        @RequestParam Integer quantity
    ) {
        return inventoryService.isInStock(skuCode, quantity);
    }

	@Operation(summary = "Check the health of the inventory service")
    @GetMapping("/health")
    public String health() {
        return "UP";
    }

 	@Operation(summary = "Create or update inventory")
    @PostMapping
    public ResponseEntity<InventoryResponse> upsertInventory(
        @Valid @RequestBody InventoryRequest request
    ) {
        return ResponseEntity.ok(
            inventoryService.upsertInventory(request)
        );
    }

	@Operation(summary = "Get inventory by SKU code")
    @GetMapping("/{skuCode}")
    public ResponseEntity<InventoryResponse> getInventoryBySkuCode(
        @PathVariable String skuCode
    ) {
        return ResponseEntity.ok(
            inventoryService.getInventoryBySkuCode(skuCode)
        );
    }

	@Operation(summary = "Decrease inventory")
    @PostMapping("/decrease")
    public ResponseEntity<InventoryResponse> decreaseInventory(
        @Valid @RequestBody InventoryRequest request
    ) {
        return ResponseEntity.ok(
            inventoryService.decreaseInventory(request)
        );
    }
}