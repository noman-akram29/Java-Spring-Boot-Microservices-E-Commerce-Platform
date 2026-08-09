package com.techie.microservices.order.client;

import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallbackFactory
        implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {

        return new InventoryClient() {

            @Override
            public boolean isInStock(String skuCode, Integer quantity) {
                return false;
            }

            @Override
            public ResponseEntity<InventoryResponse> decreaseInventory(
                    InventoryRequest request) {

                throw new RuntimeException(
                        "Inventory service unavailable",
                        cause
                );
            }
        };
    }
}