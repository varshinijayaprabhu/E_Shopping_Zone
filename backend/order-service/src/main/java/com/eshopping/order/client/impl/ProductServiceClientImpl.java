package com.eshopping.order.client.impl;

import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.dto.client.InventoryReleaseClientRequestDTO;
import com.eshopping.order.dto.client.InventoryReserveClientRequestDTO;
import com.eshopping.order.dto.client.InventoryReserveClientResponseDTO;
import com.eshopping.order.dto.client.ProductClientDTO;
import com.eshopping.order.exception.BadRequestException;
import com.eshopping.order.exception.InsufficientStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class ProductServiceClientImpl implements ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${services.product.url:http://product-service}")
    private String productServiceUrl;

    @Value("${eshopping.internal.secret-key:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecretKey;

    public ProductServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public InventoryReserveClientResponseDTO reserveStock(InventoryReserveClientRequestDTO request) {
        String url = productServiceUrl + "/api/products/reserve";
        HttpHeaders headers = createInternalHeaders();
        HttpEntity<InventoryReserveClientRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<InventoryReserveClientResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    InventoryReserveClientResponseDTO.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody();
            }
            throw new InsufficientStockException("Failed to reserve stock: " +
                    (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
        } catch (HttpStatusCodeException ex) {
            log.error("Stock reservation failed for order {}: Status {} Body {}",
                    request.getOrderNumber(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new InsufficientStockException("Insufficient inventory to fulfill order: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error communicating with product service during reservation for order {}: {}",
                    request.getOrderNumber(), ex.getMessage());
            throw new BadRequestException("Product inventory service is unavailable: " + ex.getMessage());
        }
    }

    @Override
    public void releaseStock(InventoryReleaseClientRequestDTO request) {
        String url = productServiceUrl + "/api/products/release";
        HttpHeaders headers = createInternalHeaders();
        HttpEntity<InventoryReleaseClientRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Stock successfully released for order {}", request.getOrderNumber());
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to release stock for order {}: {}", request.getOrderNumber(), ex.getMessage());
            // In production, an alert or dead-letter queue would capture this for Saga reconciliation
        }
    }

    @Override
    public ProductClientDTO getProductById(Long productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ProductClientDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductClientDTO.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.error("Failed to fetch product details for productId {}: {}", productId, ex.getMessage());
            return null;
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecretKey);
        headers.set("X-Internal-Service", "order-service");
        return headers;
    }
}

