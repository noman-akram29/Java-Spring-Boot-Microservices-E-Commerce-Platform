package com.techie.microservices.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
        "SPRING_MAIL_HOST=localhost",
        "SPRING_MAIL_PORT=1025",
        "SPRING_MAIL_USERNAME=anonymous",
        "SPRING_MAIL_PASSWORD=mailpit123"
})
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}