package com.techie.microservices.inventory.service;

import com.techie.microservices.inventory.dto.InventoryRequest;
import com.techie.microservices.inventory.dto.InventoryResponse;
import com.techie.microservices.inventory.exception.InsufficientStockException;
import com.techie.microservices.inventory.exception.ResourceNotFoundException;
import com.techie.microservices.inventory.model.Inventory;
import com.techie.microservices.inventory.repository.InventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	public boolean isInStock(String skuCode, Integer quantity) {

		log.debug(
		    "Checking inventory stock for skuCode: {} with quantity: {}",
		    skuCode,
		    quantity
		);

		return inventoryRepository
		       .existsBySkuCodeAndQuantityIsGreaterThanEqual(
		           skuCode,
		           quantity
		       );
	}

	@Transactional
	public InventoryResponse upsertInventory(InventoryRequest request) {

		log.info(
		    "Upserting inventory for skuCode: {} with quantity: {}",
		    request.skuCode(),
		    request.quantity()
		);

		int updatedCount = inventoryRepository.increaseInventoryQuantity(
		                       request.skuCode(),
		                       request.quantity()
		                   );

		inventoryRepository.flush();

		if (updatedCount == 0) {

			log.info(
			    "Inventory does not exist. Creating new inventory for skuCode: {}",
			    request.skuCode()
			);

			Inventory newInventory = new Inventory();

			newInventory.setSkuCode(request.skuCode());
			newInventory.setQuantity(request.quantity());

			Inventory savedInventory = inventoryRepository.save(newInventory);

			log.info(
			    "New inventory created successfully for skuCode: {}",
			    request.skuCode()
			);

			return mapToResponse(savedInventory);
		}

		log.info(
		    "Inventory updated successfully for skuCode: {}",
		    request.skuCode()
		);

		return inventoryRepository.findBySkuCode(request.skuCode())
		       .map(this::mapToResponse)
		       .orElseThrow(() -> new IllegalStateException(
		                        "Inventory not found after successful update for skuCode: "
		                        + request.skuCode()
		                    ));
	}

	@Transactional
	public InventoryResponse decreaseInventory(InventoryRequest request) {

		log.info(
		    "Decreasing inventory for skuCode: {} with quantity: {}",
		    request.skuCode(),
		    request.quantity()
		);

		int updatedCount = inventoryRepository.decreaseInventoryQuantity(
		                       request.skuCode(),
		                       request.quantity()
		                   );

		if (updatedCount == 0) {

			boolean inventoryExists = inventoryRepository
			                          .findBySkuCode(request.skuCode())
			                          .isPresent();

			if (!inventoryExists) {

				log.error(
				    "Inventory not found for skuCode: {}",
				    request.skuCode()
				);

				throw new ResourceNotFoundException(
				    "Inventory not found for skuCode: "
				    + request.skuCode()
				);
			}

			log.error(
			    "Insufficient stock for skuCode: {}",
			    request.skuCode()
			);

			throw new InsufficientStockException(
			    "Insufficient stock for skuCode: "
			    + request.skuCode()
			);
		}

		log.info(
		    "Inventory decreased successfully for skuCode: {}",
		    request.skuCode()
		);

		return inventoryRepository.findBySkuCode(request.skuCode())
		       .map(this::mapToResponse)
		       .orElseThrow(() -> new IllegalStateException(
		                        "Failed to retrieve updated inventory for skuCode: "
		                        + request.skuCode()
		                    ));
	}

	public InventoryResponse getInventoryBySkuCode(String skuCode) {

		log.debug(
		    "Fetching inventory for skuCode: {}",
		    skuCode
		);

		return inventoryRepository.findBySkuCode(skuCode)
		       .map(this::mapToResponse)
		       .orElseThrow(() -> new ResourceNotFoundException(
		                        "Inventory not found for skuCode: " + skuCode
		                    ));
	}

	private InventoryResponse mapToResponse(Inventory inventory) {

		return new InventoryResponse(
		           inventory.getId(),
		           inventory.getSkuCode(),
		           inventory.getQuantity()
		       );
	}
}