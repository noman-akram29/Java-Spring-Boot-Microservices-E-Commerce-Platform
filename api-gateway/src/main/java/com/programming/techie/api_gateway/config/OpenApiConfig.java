package com.programming.techie.api_gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        private static final String BEARER = "bearerAuth";

        @Bean
        public OpenAPI apiGatewayOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("API Gateway")
                                                .description("This is the API Gateway routing requests to microservices")
                                                .version("v0.0.1")
                                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                                .servers(List.of(
                                                new Server().url("/").description("API Gateway Server")))
                                .components(new Components()
                                                .addSecuritySchemes(BEARER,
                                                                new SecurityScheme()
                                                                                .name(BEARER)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")))
                                .addSecurityItem(new SecurityRequirement().addList(BEARER));
        }
}