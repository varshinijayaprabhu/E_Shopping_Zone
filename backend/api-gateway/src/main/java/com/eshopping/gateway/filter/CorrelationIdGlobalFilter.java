package com.eshopping.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global reactive filter ensuring end-to-end request traceability.
 * Generates an X-Correlation-Id if absent, injects it into downstream headers,
 * and attaches it to the client HTTP response.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new Correlation-ID: {} for path: {}", correlationId, request.getPath());
        } else {
            log.debug("Existing Correlation-ID: {} for path: {}", correlationId, request.getPath());
        }

        final String finalCorrelationId = correlationId;

        // Mutate inbound request with the correlation ID
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Attach correlation ID to outbound response
        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

