package com.eshopping.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdGlobalFilterTest {

    private CorrelationIdGlobalFilter filter;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdGlobalFilter();
    }

    @Test
    @DisplayName("Filter generates new Correlation-ID when none is provided in request")
    void testFilter_GeneratesNewCorrelationId() {
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange captured = captor.getValue();
        String reqHeader = captured.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertNotNull(reqHeader);
        assertFalse(reqHeader.trim().isEmpty());

        String resHeader = captured.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertNotNull(resHeader);
        assertEquals(reqHeader, resHeader);
    }

    @Test
    @DisplayName("Filter preserves existing Correlation-ID when present in request")
    void testFilter_PreservesExistingCorrelationId() {
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        String existingId = "CID-TEST-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange captured = captor.getValue();
        String reqHeader = captured.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals(existingId, reqHeader);

        String resHeader = captured.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals(existingId, resHeader);
    }

    @Test
    @DisplayName("Filter order has highest precedence")
    void testGetOrder() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }
}
