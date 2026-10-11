
package com.techie.microservices.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(
                DockerImageName.parse("apache/kafka:3.8.1"));
    }

    @Bean
    DynamicPropertyRegistrar kafkaProperties(KafkaContainer kafkaContainer) {
        return registry -> registry.add(
                "spring.kafka.bootstrap-servers",
                kafkaContainer::getBootstrapServers);
    }
}
