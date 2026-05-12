package com.techie.microservices.order.controller;

import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
    public String placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        boolean result = orderService.placeOrder(orderRequest);

        if (result) {
            return "Order placed successfully";
        }
        return "Product is not in stock or failed to place order";
    }
}
