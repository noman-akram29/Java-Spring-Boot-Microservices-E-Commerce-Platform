package com.programming.techie.api_gateway.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayServicePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(GatewayServiceProperties.class)
    static class TestConfig {
    }

    @Test
    void bindsAllThreeServiceUrlsFromProperties() {
        contextRunner
                .withPropertyValues(
                        "gateway.services.inventory-url=http://inventory:8081",
                        "gateway.services.product-url=http://product:8082",
                        "gateway.services.order-url=http://order:8083")
                .run(context -> {
                    GatewayServiceProperties props = context.getBean(GatewayServiceProperties.class);
                    assertThat(props.getInventoryUrl()).isEqualTo("http://inventory:8081");
                    assertThat(props.getProductUrl()).isEqualTo("http://product:8082");
                    assertThat(props.getOrderUrl()).isEqualTo("http://order:8083");
                });
    }
}