package com.techie.microservices.order.service;

import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.dto.OrderRequest.UserDetails;
import com.techie.microservices.order.external.dto.InventoryRequest;
import com.techie.microservices.order.external.dto.InventoryResponse;
import com.techie.microservices.order.model.IdempotencyRecord;
import com.techie.microservices.order.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private IdempotencyRepository idempotencyRepository;

        @Mock
        private InventoryClient inventoryClient;

        @Mock
        private OrderTransactionalOperations orderTransactionalOperations;

        @InjectMocks
        private OrderService orderService;

        private OrderRequest orderRequest;
        private final String idempotencyKey = "test-idempotency-key-123";

        @BeforeEach
        void setUp() {
                orderRequest = new OrderRequest(
                                null,
                                null,
                                "charger_x1",
                                "charger_x1",
                                5,
                                new BigDecimal("100.00"),
                                new UserDetails("test@example.com", "Test", "User"));
        }

        @Test
        void shouldReturnFailedWhenInventoryReservationFails() {
                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
                when(orderTransactionalOperations.tryReserveIdempotencyKey(eq(idempotencyKey), anyString()))
                                .thenReturn(true);
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException("connection refused"));

                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                assertThat(response.status()).isEqualTo("FAILED");
                verify(orderTransactionalOperations, never()).saveOrderAndOutbox(anyString(), any());
                verify(idempotencyRepository).deleteByIdempotencyKey(idempotencyKey);
        }

        @Test
        void shouldReturnFailedWhenInventoryReturnsNon2xx() {
                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
                when(orderTransactionalOperations.tryReserveIdempotencyKey(eq(idempotencyKey), anyString()))
                                .thenReturn(true);
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenReturn(ResponseEntity.badRequest().build());

                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                assertThat(response.status()).isEqualTo("FAILED");
                verify(orderTransactionalOperations, never()).saveOrderAndOutbox(anyString(), any());
                verify(idempotencyRepository).deleteByIdempotencyKey(idempotencyKey);
        }

        @Test
        void shouldSaveOrderAndOutboxWhenInventorySucceeds() {
                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
                when(orderTransactionalOperations.tryReserveIdempotencyKey(eq(idempotencyKey), anyString()))
                                .thenReturn(true);

                InventoryResponse invResponse = new InventoryResponse(1L, "charger_x1", 10);
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenReturn(ResponseEntity.ok(invResponse));

                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                assertThat(response.status()).isEqualTo("SUCCESS");
                assertThat(response.orderNumber()).isNotNull();
                verify(orderTransactionalOperations).saveOrderAndOutbox(anyString(), eq(orderRequest));
        }

        @Test
        void shouldReplayWhenIdempotencyKeyAlreadyExists() {
                IdempotencyRecord existing = new IdempotencyRecord();
                existing.setIdempotencyKey(idempotencyKey);
                existing.setOrderNumber("existing-order-123");
                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(Optional.of(existing));

                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                assertThat(response.status()).isEqualTo("SUCCESS");
                assertThat(response.orderNumber()).isEqualTo("existing-order-123");
                verify(inventoryClient, never()).decreaseInventory(any());
                verify(orderTransactionalOperations, never()).tryReserveIdempotencyKey(anyString(), anyString());
        }

        @Test
        void shouldCompensateInventoryAndReleaseKey_whenSaveOrderAndOutboxFails() {
                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
                when(orderTransactionalOperations.tryReserveIdempotencyKey(eq(idempotencyKey), anyString()))
                                .thenReturn(true);

                InventoryResponse invResponse = new InventoryResponse(1L, "charger_x1", 10);
                when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                                .thenReturn(ResponseEntity.ok(invResponse));

                doThrow(new RuntimeException("DB connection lost"))
                                .when(orderTransactionalOperations).saveOrderAndOutbox(anyString(), eq(orderRequest));

                OrderResponse response = orderService.placeOrder(idempotencyKey, orderRequest);

                assertThat(response.status()).isEqualTo("FAILED");
                assertThat(response.message()).contains("restored");
                verify(inventoryClient).increaseInventory(new InventoryRequest("charger_x1", 5));
                verify(idempotencyRepository).deleteByIdempotencyKey(idempotencyKey);
        }
}