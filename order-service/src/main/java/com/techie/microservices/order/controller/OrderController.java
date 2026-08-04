package com.techie.microservices.order.controller;

import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.dto.OrderResponse;
import com.techie.microservices.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping("/")
	public String home() {
		return "Order Service is running...";
	}

	@GetMapping("/health")
	public String health() {
		return "UP";
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
		return orderService.placeOrder(orderRequest);
	}
}