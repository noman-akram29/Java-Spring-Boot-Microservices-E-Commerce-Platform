package com.programming.techie.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	private static final String CORRELATION_ID = "Correlation-Id";

	@Override
	public Mono<Void> filter(
			ServerWebExchange exchange,
			GatewayFilterChain chain) {

		String correlationId = exchange.getRequest()
				.getHeaders()
				.getFirst(CORRELATION_ID);

		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		String path = exchange.getRequest()
				.getURI()
				.getPath();

		String method = exchange.getRequest()
				.getMethod()
				.name();

		log.info(
			"Incoming Request: {} {} | CorrelationId: {}",
			method,
			path,
			correlationId
		);

		exchange.getResponse()
				.getHeaders()
				.add(
					CORRELATION_ID,
					correlationId
				);

		ServerWebExchange modifiedExchange =
				exchange.mutate()
						.request(
							exchange.getRequest()
							.mutate()
							.header(
								CORRELATION_ID,
								correlationId
							)
							.build()
						)
						.build();

		return chain.filter(modifiedExchange);
	}
}