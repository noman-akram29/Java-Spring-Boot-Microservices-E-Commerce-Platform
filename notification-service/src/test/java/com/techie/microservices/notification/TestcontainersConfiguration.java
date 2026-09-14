package com.techie.microservices.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        // 3.8.1 is more reliable with Testcontainers on Docker Desktop than 3.9.0
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));
    }
}