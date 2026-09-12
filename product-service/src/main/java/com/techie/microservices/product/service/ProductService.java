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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

		InventoryResponse inventoryResponse = inventoryClient.upsertInventory(
				new InventoryRequest(
						productRequest.skuCode(),
						productRequest.quantity()))
				.getBody();

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
				inventoryResponse.quantity());
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> getAllProducts() {

		List<Product> products = productRepository.findAll();
		List<String> skuCodes = products.stream().map(Product::getSkuCode).toList();

		Map<String, Integer> quantityBySku;

		try {
			List<InventoryResponse> responses = inventoryClient.getInventoryBySkuCodes(skuCodes).getBody();
			if (responses != null) {
				quantityBySku = responses.stream()
						.collect(Collectors.toMap(InventoryResponse::skuCode, InventoryResponse::quantity));
			} else {
				quantityBySku = Map.of();
			}
		} catch (Exception ex) {
			log.error("Failed to fetch inventory batch for {} SKUs", skuCodes.size(), ex);
			quantityBySku = Map.of();
		}

		Map<String, Integer> finalMap = quantityBySku;

		return products.stream()
				.map(product -> new ProductResponse(
						product.getId(),
						product.getName(),
						product.getDescription(),
						product.getPrice(),
						finalMap.getOrDefault(product.getSkuCode(), 0)))
				.toList();
	}
}