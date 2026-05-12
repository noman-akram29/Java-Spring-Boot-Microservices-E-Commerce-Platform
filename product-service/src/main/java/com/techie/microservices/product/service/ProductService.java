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

        // 1. Build Product
        Product product = Product.builder()
                .skuCode(productRequest.skuCode())
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .build();

        // 2. Save Product
        productRepository.save(product);
        log.info("Product created successfully: {}", product);

        // 3. Call Inventory Service
        InventoryResponse inventoryResponse = inventoryClient
                .upsertInventory(new InventoryRequest(
                        productRequest.skuCode(),
                        productRequest.quantity()
                ))
                .getBody();

        if (inventoryResponse == null) {
            throw new RuntimeException("Inventory service returned null response");
        }

        log.info("Inventory updated successfully: {}", inventoryResponse);

        // 4. Return response
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
                    InventoryResponse inventoryResponse = inventoryClient
                            .getInventoryBySkuCode(product.getSkuCode())
                            .getBody();

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