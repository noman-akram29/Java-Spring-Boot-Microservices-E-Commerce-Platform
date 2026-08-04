package com.techie.microservices.inventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleResourceNotFound(
	    ResourceNotFoundException ex,
	    HttpServletRequest request
	) {

		ApiError error = new ApiError(
		    LocalDateTime.now(),
		    HttpStatus.NOT_FOUND.value(),
		    "NOT_FOUND",
		    ex.getMessage(),
		    request.getRequestURI()
		);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ApiError> handleInsufficientStock(
	    InsufficientStockException ex,
	    HttpServletRequest request
	) {

		ApiError error = new ApiError(
		    LocalDateTime.now(),
		    HttpStatus.BAD_REQUEST.value(),
		    "INSUFFICIENT_STOCK",
		    ex.getMessage(),
		    request.getRequestURI()
		);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleIllegalState(
	    IllegalStateException ex,
	    HttpServletRequest request
	) {

		ApiError error = new ApiError(
		    LocalDateTime.now(),
		    HttpStatus.INTERNAL_SERVER_ERROR.value(),
		    "ILLEGAL_STATE",
		    ex.getMessage(),
		    request.getRequestURI()
		);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGenericException(
	    Exception ex,
	    HttpServletRequest request
	) {

		ApiError error = new ApiError(
		    LocalDateTime.now(),
		    HttpStatus.INTERNAL_SERVER_ERROR.value(),
		    "INTERNAL_ERROR",
		    ex.getMessage(),
		    request.getRequestURI()
		);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}