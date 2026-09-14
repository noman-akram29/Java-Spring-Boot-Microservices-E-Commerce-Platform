package com.programming.techie.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter implements GlobalFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final String CORRELATION_ID = "Correlation-Id";
	private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9-]");
	private static final int MAX_LEN = 64;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String incoming = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
		String correlationId = sanitize(incoming);

		String path = exchange.getRequest().getURI().getPath();
		String method = exchange.getRequest().getMethod().name();

		log.info("Incoming Request: {} {} | CorrelationId: {}", method, path, correlationId);

		exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);

		ServerWebExchange modifiedExchange = exchange.mutate()
				.request(exchange.getRequest().mutate()
						.header(CORRELATION_ID, correlationId) // downstream services now only ever see the sanitized
																// value
						.build())
				.build();

		return chain.filter(modifiedExchange);
	}

	private String sanitize(String raw) {
		if (raw == null || raw.isBlank()) {
			return UUID.randomUUID().toString();
		}
		String cleaned = UNSAFE_CHARS.matcher(raw).replaceAll("");
		if (cleaned.isEmpty()) {
			return UUID.randomUUID().toString();
		}
		return cleaned.substring(0, Math.min(cleaned.length(), MAX_LEN));
	}
}