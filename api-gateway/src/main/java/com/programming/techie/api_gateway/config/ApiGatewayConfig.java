package com.programming.techie.api_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfig {

	private final GatewayFilter swaggerServerRewrite;

	public ApiGatewayConfig(GatewayFilter swaggerServerRewrite) {
		this.swaggerServerRewrite = swaggerServerRewrite;
	}

	@Bean
	public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {

		return builder.routes()

			.route("inventory-service", r -> r
				.path("/inventory-service/**")
				.filters(f -> f
					.rewritePath(
						"/inventory-service/(?<segment>.*)",
						"/${segment}"
					)
					.filter(swaggerServerRewrite)
				)
				.uri("http://localhost:8081"))

			.route("product-service", r -> r
				.path("/product-service/**")
				.filters(f -> f
					.rewritePath(
						"/product-service/(?<segment>.*)",
						"/${segment}"
					)
					.filter(swaggerServerRewrite)
				)
				.uri("http://localhost:8082"))

			.route("order-service", r -> r
				.path("/order-service/**")
				.filters(f -> f
					.rewritePath(
						"/order-service/(?<segment>.*)",
						"/${segment}"
					)
					.filter(swaggerServerRewrite)
				)
				.uri("http://localhost:8083"))

			.build();
	}
}