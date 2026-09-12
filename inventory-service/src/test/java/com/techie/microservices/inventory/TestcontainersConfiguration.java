package com.techie.microservices.inventory;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration(proxyBeanMethods = false)
@ActiveProfiles("test")
class TestcontainersConfiguration {

    // @Bean
    // @ServiceConnection
    // MySQLContainer<?> mysqlContainer() {
    // return new MySQLContainer<>(DockerImageName.parse("mysql:latest"));
    // }

}
