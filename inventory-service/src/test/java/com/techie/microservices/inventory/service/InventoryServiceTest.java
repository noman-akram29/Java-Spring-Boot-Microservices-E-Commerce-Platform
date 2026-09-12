package com.techie.microservices.inventory.service;

import com.techie.microservices.inventory.dto.InventoryRequest;
import com.techie.microservices.inventory.dto.InventoryResponse;
import com.techie.microservices.inventory.exception.InsufficientStockException;
import com.techie.microservices.inventory.exception.ResourceNotFoundException;
import com.techie.microservices.inventory.model.Inventory;
import com.techie.microservices.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void isInStock_delegatesToRepositoryCheck() {
        when(inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual("sku-1", 5))
                .thenReturn(true);

        assertThat(inventoryService.isInStock("sku-1", 5)).isTrue();
        verify(inventoryRepository).existsBySkuCodeAndQuantityIsGreaterThanEqual("sku-1", 5);
    }

    @Test
    void upsertInventory_createsNewRowWhenNoneExists() {
        when(inventoryRepository.increaseInventoryQuantity("sku-1", 10)).thenReturn(0);

        Inventory saved = new Inventory(1L, "sku-1", 10);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(saved);

        InventoryResponse response = inventoryService.upsertInventory(new InventoryRequest("sku-1", 10));

        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.skuCode()).isEqualTo("sku-1");
        verify(inventoryRepository)
                .save(argThat(inv -> inv.getSkuCode().equals("sku-1") && inv.getQuantity().equals(10)));
    }

    @Test
    void upsertInventory_updatesExistingRowInPlace() {
        when(inventoryRepository.increaseInventoryQuantity("sku-1", 5)).thenReturn(1);
        when(inventoryRepository.findBySkuCode("sku-1"))
                .thenReturn(Optional.of(new Inventory(1L, "sku-1", 15)));

        InventoryResponse response = inventoryService.upsertInventory(new InventoryRequest("sku-1", 5));

        assertThat(response.quantity()).isEqualTo(15);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void decreaseInventory_succeedsWhenEnoughStock() {
        when(inventoryRepository.decreaseInventoryQuantity("sku-1", 3)).thenReturn(1);
        when(inventoryRepository.findBySkuCode("sku-1"))
                .thenReturn(Optional.of(new Inventory(1L, "sku-1", 7)));

        InventoryResponse response = inventoryService.decreaseInventory(new InventoryRequest("sku-1", 3));

        assertThat(response.quantity()).isEqualTo(7);
    }

    @Test
    void decreaseInventory_throwsInsufficientStock_whenSkuExistsButNotEnoughQuantity() {
        when(inventoryRepository.decreaseInventoryQuantity("sku-1", 100)).thenReturn(0);
        when(inventoryRepository.findBySkuCode("sku-1"))
                .thenReturn(Optional.of(new Inventory(1L, "sku-1", 3)));

        assertThatThrownBy(() -> inventoryService.decreaseInventory(new InventoryRequest("sku-1", 100)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void decreaseInventory_throwsResourceNotFound_whenSkuDoesNotExistAtAll() {
        when(inventoryRepository.decreaseInventoryQuantity("unknown-sku", 1)).thenReturn(0);
        when(inventoryRepository.findBySkuCode("unknown-sku")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.decreaseInventory(new InventoryRequest("unknown-sku", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInventoryBySkuCode_throwsResourceNotFound_whenMissing() {
        when(inventoryRepository.findBySkuCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryBySkuCode("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}