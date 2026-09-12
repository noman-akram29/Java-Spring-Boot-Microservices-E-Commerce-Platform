package com.techie.microservices.product.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {

                String message = exception
                                .getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getField() + " " + error.getDefaultMessage())
                                .orElse("Validation failed");

                return ResponseEntity
                                .badRequest()
                                .body(new ErrorResponse(
                                                LocalDateTime.now(),
                                                400,
                                                "VALIDATION_ERROR",
                                                message,
                                                request.getRequestURI()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception exception,
                        HttpServletRequest request) {

                log.error("Unhandled exception on {}", request.getRequestURI(), exception);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ErrorResponse(
                                                LocalDateTime.now(),
                                                500,
                                                "INTERNAL_ERROR",
                                                "An unexpected error occurred. Please contact support with the correlation ID from the response headers.",
                                                request.getRequestURI()));
        }
}