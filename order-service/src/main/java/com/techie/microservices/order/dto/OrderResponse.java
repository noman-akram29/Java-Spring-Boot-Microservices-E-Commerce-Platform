package com.techie.microservices.order.dto;

public record OrderResponse(
    String orderNumber,
    String status,
    String message
) {
}