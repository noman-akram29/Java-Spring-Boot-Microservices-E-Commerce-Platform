package com.programming.techie.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.programming.techie.api_gateway.config.properties.GatewayServiceProperties;

@SpringBootApplication
@EnableConfigurationProperties(GatewayServiceProperties.class)
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}