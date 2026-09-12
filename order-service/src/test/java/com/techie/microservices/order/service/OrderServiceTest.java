package com.techie.microservices.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.dto.OrderRequest.UserDetails;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.IdempotencyRecord;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.model.OutboxEvent;
import com.techie.microservices.order.repository.IdempotencyRepository;
import com.techie.microservices.order.repository.OrderRepository;
import com.techie.microservices.order.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private OutboxRepository outboxRepository;

        @Mock
        private IdempotencyRepository idempotencyRepository;

        @Mock
        private InventoryClient inventoryClient;

        @Mock
        private ObjectMapper objectMapper;

        @InjectMocks
        private OrderService orderService;

        private OrderRequest orderRequest;
        private final String idempotencyKey = "test-idempotency-key-123";

        @BeforeEach
        void setUp() {
                orderRequest = new OrderRequest(
                                null, // id
                                null, // orderNumber
                                "charger_x1", // skuCode
                                "charger_x1", // productId
                                5, // quantity
                                new BigDecimal("100.00"), // price
                                new UserDetails("test@example.com", "Test", "User"));
        }

        @Test
        void shouldReturnFailedWhenInventoryReservationFails() {
                // given
                when(idempotencyRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException("Insufficient stock"));

                // when
                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                // then
                assertThat(response.status()).isEqualTo("FAILED");
                assertThat(response.message()).contains("out of stock");
                assertThat(response.orderNumber()).isNull();

                verify(orderRepository, never()).save(any());
                verify(outboxRepository, never()).save(any());
                // Idempotency record is saved upfront and then cleaned up via compensating
                // delete on failure
                verify(idempotencyRepository, atLeastOnce()).save(any(IdempotencyRecord.class));
                verify(idempotencyRepository, atLeastOnce()).delete(any(IdempotencyRecord.class));
        }

        @Test
        void shouldReturnFailedWhenInventoryReturnsNon2xx() {
                // given
                when(idempotencyRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenReturn(ResponseEntity.badRequest().build());

                // when
                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                // then
                assertThat(response.status()).isEqualTo("FAILED");
                assertThat(response.orderNumber()).isNull();

                verify(orderRepository, never()).save(any());
                verify(outboxRepository, never()).save(any());
                // Idempotency record is saved upfront and then cleaned up via compensating
                // delete on failure
                verify(idempotencyRepository, atLeastOnce()).save(any(IdempotencyRecord.class));
                verify(idempotencyRepository, atLeastOnce()).delete(any(IdempotencyRecord.class));
        }

        @Test
        void shouldSaveOrderAndOutboxWhenInventorySucceeds() throws Exception {
                // given
                when(idempotencyRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

                InventoryResponse invResponse = new InventoryResponse(1L, "charger_x1", 10);
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenReturn(ResponseEntity.ok(invResponse));

                when(objectMapper.writeValueAsString(any()))
                                .thenReturn("{\"orderNumber\":\"123\",\"email\":\"test@example.com\"}");

                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // when
                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                // then
                assertThat(response.status()).isEqualTo("SUCCESS");
                assertThat(response.orderNumber()).isNotNull();

                // Order was saved
                ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
                verify(orderRepository).save(orderCaptor.capture());
                assertThat(orderCaptor.getValue().getSkuCode()).isEqualTo("charger_x1");
                assertThat(orderCaptor.getValue().getQuantity()).isEqualTo(5);

                // Outbox was saved
                ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
                verify(outboxRepository).save(outboxCaptor.capture());
                assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("OrderPlaced");
                assertThat(outboxCaptor.getValue().getAggregateType()).isEqualTo("Order");

                // Idempotency record was saved
                verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        }
}