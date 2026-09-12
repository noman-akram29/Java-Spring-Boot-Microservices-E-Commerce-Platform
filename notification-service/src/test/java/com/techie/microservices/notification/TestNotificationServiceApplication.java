package com.techie.microservices.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
        "SPRING_MAIL_HOST=localhost",
        "SPRING_MAIL_PORT=1025",
        "SPRING_MAIL_USERNAME=anonymous",
        "SPRING_MAIL_PASSWORD=mailpit123"
})
class TestNotificationServiceApplication {

    @Test
    void contextLoads() {
    }

}