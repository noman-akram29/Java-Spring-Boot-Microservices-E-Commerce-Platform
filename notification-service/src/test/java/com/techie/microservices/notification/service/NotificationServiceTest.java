package com.techie.microservices.notification.service;

import com.techie.microservices.notification.event.OrderPlacedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listen_sendsEmail_whenEventReceived() {
        OrderPlacedEvent event = new OrderPlacedEvent("order-789", "buyer@example.com");

        notificationService.listen(event);

        verify(javaMailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void listen_swallowsException_soKafkaConsumerNeverDies() {
        OrderPlacedEvent event = new OrderPlacedEvent("order-999", "buyer@example.com");

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(javaMailSender).send(any(MimeMessagePreparator.class));

        // must NOT throw — this is the whole point of the try/catch in
        // NotificationService
        notificationService.listen(event);

        verify(javaMailSender, times(1)).send(any(MimeMessagePreparator.class));
    }
}