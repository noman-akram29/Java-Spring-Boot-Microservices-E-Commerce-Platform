package com.programming.techie.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfig {

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {

        return builder.routes()

                // PRODUCT
                .route("product-service", r -> r
                        .path("/api/product/**")
                        .uri(productServiceUrl))

                // ORDER
                .route("order-service", r -> r
                        .path("/api/order/**")
                        .uri(orderServiceUrl))

                // INVENTORY
                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .uri(inventoryServiceUrl))

                .build();
    }
}