package com.eshopping.admin.client.impl;

import com.eshopping.admin.client.OrderServiceClient;
import com.eshopping.admin.dto.ApiResponse;
import com.eshopping.admin.dto.OrderMetricsSummaryDTO;
import com.eshopping.admin.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class OrderServiceClientImpl implements OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${services.order.url:http://order-service}")
    private String orderServiceUrl;

    @Value("${internal.service.secret:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecret;

    public OrderServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);
        return headers;
    }

    @Override
    public OrderMetricsSummaryDTO getMetricsSummary() {
        String url = orderServiceUrl + "/api/orders/internal/metrics/summary";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            ResponseEntity<ApiResponse<OrderMetricsSummaryDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<OrderMetricsSummaryDTO>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return new OrderMetricsSummaryDTO(0, BigDecimal.ZERO);
        } catch (Exception ex) {
            log.error("Failed to fetch metrics summary from order-service: {}", ex.getMessage());
            return new OrderMetricsSummaryDTO(0, BigDecimal.ZERO);
        }
    }

    @Override
    public void adminDisputeRefund(Long orderId, String reason) {
        String url = orderServiceUrl + "/api/orders/internal/" + orderId + "/dispute-refund?reason=" + (reason != null ? reason : "");
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("Successfully triggered dispute refund for order {}", orderId);
        } catch (Exception ex) {
            log.error("Failed to trigger dispute refund in order-service for order {}: {}", orderId, ex.getMessage());
            throw new BadRequestException("Failed to process dispute refund: " + ex.getMessage());
        }
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        String url = orderServiceUrl + "/api/orders/internal/" + orderId + "/status?status=" + status;
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Successfully updated order {} status to {}", orderId, status);
        } catch (Exception ex) {
            log.error("Failed to update status for order {}: {}", orderId, ex.getMessage());
            throw new BadRequestException("Failed to update order status: " + ex.getMessage());
        }
    }
}
