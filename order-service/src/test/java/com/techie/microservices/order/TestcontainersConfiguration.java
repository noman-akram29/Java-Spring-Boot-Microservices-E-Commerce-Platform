package com.techie.microservices.order;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                .withDatabaseName("order_db")
                .withUsername("order_app")
                .withPassword("test");
    }

    /**
     * Official Apache Kafka image (KRaft).
     * If this fails on Windows Docker, try: apache/kafka-native:3.8.1
     * or fall back to confluentinc/cp-kafka via
     * org.testcontainers.containers.KafkaContainer.
     */
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));
    }
}