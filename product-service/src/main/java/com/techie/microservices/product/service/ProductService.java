package com.techie.microservices.product.service;

import com.techie.microservices.product.dto.ProductRequest;
import com.techie.microservices.product.dto.ProductResponse;
import com.techie.microservices.product.external.client.InventoryClient;
import com.techie.microservices.product.external.dto.InventoryRequest;
import com.techie.microservices.product.external.dto.InventoryResponse;
import com.techie.microservices.product.model.Product;
import com.techie.microservices.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

	private final ProductRepository productRepository;
	private final InventoryClient inventoryClient;

	public ProductResponse createProduct(ProductRequest productRequest) {

		Product product = Product.builder()
		                  .skuCode(productRequest.skuCode())
		                  .name(productRequest.name())
		                  .description(productRequest.description())
		                  .price(productRequest.price())
		                  .build();

		productRepository.save(product);

		log.info("Product saved successfully: {}", product.getSkuCode());

		InventoryResponse inventoryResponse =
		    inventoryClient.upsertInventory(
		        new InventoryRequest(
		            productRequest.skuCode(),
		            productRequest.quantity()
		        )
		    ).getBody();

		if (inventoryResponse == null) {
			log.error("Inventory service returned null response for SKU: {}", productRequest.skuCode());
			throw new IllegalStateException("Inventory service failure");
		}

		log.info("Inventory updated for SKU: {}", productRequest.skuCode());

		return new ProductResponse(
		           product.getId(),
		           product.getName(),
		           product.getDescription(),
		           product.getPrice(),
		           inventoryResponse.quantity()
		       );
	}

	public List<ProductResponse> getAllProducts() {

		List<Product> products = productRepository.findAll();

		return products.stream()
		.map(product -> {

			InventoryResponse inventoryResponse = null;

			try {
				inventoryResponse = inventoryClient
				.getInventoryBySkuCode(product.getSkuCode())
				.getBody();
			} catch (Exception ex) {
				log.error("Failed to fetch inventory for SKU: {}", product.getSkuCode(), ex);
			}

			Integer quantity = (inventoryResponse != null)
			? inventoryResponse.quantity()
			: 0;

			return new ProductResponse(
			    product.getId(),
			    product.getName(),
			    product.getDescription(),
			    product.getPrice(),
			    quantity
			);
		})
		.toList();
	}
}