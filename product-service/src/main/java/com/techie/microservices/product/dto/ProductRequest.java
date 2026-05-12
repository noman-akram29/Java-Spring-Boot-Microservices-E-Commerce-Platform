package com.techie.microservices.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "skuCode is required")
        String skuCode,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.1", message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity cannot be negative")
        Integer quantity
) {
}