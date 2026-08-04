package com.programming.techie.api_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerResponseFilter {

    @Bean
    public GatewayFilter inventorySwaggerRewrite(
            ModifyResponseBodyGatewayFilterFactory factory) {

        return factory.apply(
            new ModifyResponseBodyGatewayFilterFactory.Config()
                .setRewriteFunction(
                    String.class,
                    String.class,
                    (exchange, body) -> {

                        if (body == null) {
                            return null;
                        }

                        body = body.replace(
                            "\"url\":\"http://localhost:8081\"",
                            "\"url\":\"http://localhost:8080/inventory-service\""
                        );

                        body = body.replace(
                            "\"url\":\"http://localhost:8082\"",
                            "\"url\":\"http://localhost:8080/product-service\""
                        );

                        body = body.replace(
                            "\"url\":\"http://localhost:8083\"",
                            "\"url\":\"http://localhost:8080/order-service\""
                        );

                        return reactor.core.publisher.Mono.just(body);
                    }
                )
        );
    }
}