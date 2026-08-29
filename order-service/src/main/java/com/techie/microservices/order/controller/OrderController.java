package com.techie.microservices.order.controller;

import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Order API",
    description = "Manage Orders"
)
@RestController
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@Operation(summary = "Check if the order service is running")
    @GetMapping("/")
    public String home() {
        return "Order Service is running...";
    }

    @Operation(summary = "Check the health of the order service")
    @GetMapping("/health")
    public String health() {
        return "UP";
    }

    @Operation(summary = "Create a new order")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
		@Valid @RequestBody OrderRequest orderRequest
	) {
        return orderService.placeOrder(orderRequest);
    }
}