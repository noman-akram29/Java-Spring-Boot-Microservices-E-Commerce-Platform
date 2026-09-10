package com.techie.microservices.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.model.OutboxEvent;
import com.techie.microservices.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findUnprocessedEvents();

        for (OutboxEvent event : events) {
            try {
                OrderPlacedEvent payload =
                        objectMapper.readValue(event.getPayload(), OrderPlacedEvent.class);

                kafkaTemplate.send("order-placed", payload).get(); // wait for ack

                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);

                log.info("Outbox event published: {}", event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}", event.getId(), e);
                // leave processed=false → will retry on next poll
            }
        }
    }
}