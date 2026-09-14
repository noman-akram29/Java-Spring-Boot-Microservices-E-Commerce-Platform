package com.techie.microservices.order.client;

import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public boolean isInStock(String skuCode, Integer quantity) {
                return false;
            }

            @Override
            public ResponseEntity<InventoryResponse> decreaseInventory(InventoryRequest request) {
                // Return non-2xx so OrderService can handle gracefully (no uncaught exception)
                return ResponseEntity.status(503).build();
            }

            @Override
            public ResponseEntity<InventoryResponse> increaseInventory(InventoryRequest request) {
                return ResponseEntity.status(503).build();
            }
        };
    }
}