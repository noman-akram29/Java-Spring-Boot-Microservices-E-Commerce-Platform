package com.techie.microservices.inventory.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = sanitize(request.getHeader(HEADER));

        MDC.put(HEADER, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(HEADER);
        }
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "missing";
        }
        String cleaned = raw.replaceAll("[^a-zA-Z0-9-]", "");
        return cleaned.isEmpty() ? "missing" : cleaned.substring(0, Math.min(cleaned.length(), 64));
    }
}