package com.techie.microservices.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderRequest(
    Long id,
    String orderNumber,
    @NotBlank(message = "SKU code is required")
    String skuCode,
    String productId,
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    BigDecimal price,
    @Valid
    @NotNull(message = "User details are required")
    UserDetails userDetails

) {
	public record UserDetails(

	    @Email(message = "Invalid email format")
	    @NotBlank(message = "Email is required")
	    String email,

	    @NotBlank(message = "First name is required")
	    String firstName,

	    @NotBlank(message = "Last name is required")
	    String lastName
	) {
	}
}