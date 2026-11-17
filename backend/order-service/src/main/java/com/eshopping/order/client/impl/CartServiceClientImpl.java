package com.eshopping.order.client.impl;

import com.eshopping.order.client.CartServiceClient;
import com.eshopping.order.dto.ApiResponse;
import com.eshopping.order.dto.client.CartClientDTO;
import com.eshopping.order.exception.BadRequestException;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class CartServiceClientImpl implements CartServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CartServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${services.cart.url:http://cart-service}")
    private String cartServiceUrl;

    public CartServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CartClientDTO getCart(Long customerId, String authHeader) {
        String url = cartServiceUrl + "/api/cart";
        HttpHeaders headers = createHeaders(customerId, authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<CartClientDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<CartClientDTO>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            throw new BadRequestException("Shopping cart is empty or could not be retrieved");
        } catch (HttpStatusCodeException ex) {
            log.error("Failed to retrieve cart for customer {}: Status {} Body {}", customerId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to retrieve shopping cart: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error communicating with cart service for customer {}: {}", customerId, ex.getMessage());
            throw new BadRequestException("Shopping cart service is currently unavailable: " + ex.getMessage());
        }
    }

    @Override
    public void clearCart(Long customerId, String authHeader) {
        String url = cartServiceUrl + "/api/cart";
        HttpHeaders headers = createHeaders(customerId, authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Successfully cleared cart for customer {}", customerId);
        } catch (Exception ex) {
            log.warn("Failed to clear shopping cart for customer {} after checkout: {}", customerId, ex.getMessage());
            // Non-critical: do not abort or roll back an already placed order
        }
    }

    private HttpHeaders createHeaders(Long customerId, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(authHeader)) {
            headers.set("Authorization", authHeader);
        }
        if (customerId != null) {
            headers.set("X-User-Id", String.valueOf(customerId));
            headers.set("X-User-Role", "ROLE_CUSTOMER");
        }
        return headers;
    }
}

