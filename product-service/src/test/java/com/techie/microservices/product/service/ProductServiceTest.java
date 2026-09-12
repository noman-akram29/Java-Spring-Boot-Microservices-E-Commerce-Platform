package com.techie.microservices.product.service;

import com.techie.microservices.product.dto.ProductRequest;
import com.techie.microservices.product.dto.ProductResponse;
import com.techie.microservices.product.external.client.InventoryClient;
import com.techie.microservices.product.external.dto.InventoryResponse;
import com.techie.microservices.product.model.Product;
import com.techie.microservices.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        List<Product> products = List.of(
                Product.builder().id("1").skuCode("sku-1").name("A").description("d").price(BigDecimal.TEN).build(),
                Product.builder().id("2").skuCode("sku-2").name("B").description("d").price(BigDecimal.TEN).build(),
                Product.builder().id("3").skuCode("sku-3").name("C").description("d").price(BigDecimal.TEN).build());
        when(productRepository.findAll()).thenReturn(products);
    }

    @Test
    void getAllProducts_mustCallInventoryExactlyOnce_regardlessOfProductCount() {
        // given
        when(inventoryClient.getInventoryBySkuCodes(anyList())).thenReturn(
                ResponseEntity.ok(List.of(
                        new InventoryResponse(1L, "sku-1", 5),
                        new InventoryResponse(2L, "sku-2", 0),
                        new InventoryResponse(3L, "sku-3", 12))));

        // when
        List<ProductResponse> result = productService.getAllProducts();

        // then: exactly ONE remote call no matter how many products (N+1 regression
        // guard)
        verify(inventoryClient, times(1)).getInventoryBySkuCodes(anyList());
        verify(inventoryClient, never()).getInventoryBySkuCode(anyString());
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductResponse::quantity).containsExactly(5, 0, 12);
    }

    @Test
    void getAllProducts_defaultsToZeroQuantity_whenInventoryCallFails() {
        // given
        when(inventoryClient.getInventoryBySkuCodes(anyList()))
                .thenThrow(new RuntimeException("inventory-service unreachable"));

        // when
        List<ProductResponse> result = productService.getAllProducts();

        // then: degrade gracefully instead of failing the whole product listing
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(p -> assertThat(p.quantity()).isEqualTo(0));
    }
}