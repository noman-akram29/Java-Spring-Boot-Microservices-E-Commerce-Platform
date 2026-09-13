package com.techie.microservices.notification;

import com.techie.microservices.notification.event.OrderPlacedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @MockBean
    private JavaMailSender javaMailSender; // real SMTP is never touched in this test

    @Test
    void shouldConsumeOrderPlacedEventAndAttemptToSendEmail() {
        OrderPlacedEvent event = new OrderPlacedEvent("order-123", "customer@example.com");

        kafkaTemplate.send("order-placed", event);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessagePreparator.class)));
    }

    @Test
    void shouldNotCrashConsumer_whenMailSendingThrows() {
        doThrow(new RuntimeException("SMTP down"))
                .when(javaMailSender).send(any(MimeMessagePreparator.class));

        OrderPlacedEvent event = new OrderPlacedEvent("order-456", "another@example.com");
        kafkaTemplate.send("order-placed", event);

        // consumer must not die / enter a retry loop — it just logs and moves on
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(javaMailSender, atLeastOnce()).send(any(MimeMessagePreparator.class)));
    }
}