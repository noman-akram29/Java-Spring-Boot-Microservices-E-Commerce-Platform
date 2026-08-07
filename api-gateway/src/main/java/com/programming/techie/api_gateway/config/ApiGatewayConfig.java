package com.programming.techie.api_gateway.config;

import com.programming.techie.api_gateway.config.properties.GatewayServiceProperties;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfig {

    private final GatewayServiceProperties properties;

    public ApiGatewayConfig(
            GatewayServiceProperties properties) {

        this.properties = properties;
    }


    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                .route("product-service", r -> r
                        .path("/api/product", "/api/product/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(properties.getProductUrl()))

                .route("inventory-service", r -> r
                        .path("/api/inventory", "/api/inventory/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(properties.getInventoryUrl()))

                .route("order-service", r -> r
                        .path("/api/order", "/api/order/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(properties.getOrderUrl()))

                .build();
    }
}