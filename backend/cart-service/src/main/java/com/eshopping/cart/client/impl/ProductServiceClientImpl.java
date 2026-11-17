package com.eshopping.cart.client.impl;

import com.eshopping.cart.client.ProductServiceClient;
import com.eshopping.cart.dto.ProductClientDTO;
import com.eshopping.cart.exception.ProductServiceUnavailableException;
import com.eshopping.cart.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductServiceClientImpl implements ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${eshopping.services.product-service.url:http://product-service}")
    private String productServiceUrl;

    public ProductServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ProductClientDTO.ProductData getProductDetails(Long productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        log.debug("Calling Product Service via RestTemplate: {}", url);

        try {
            ProductClientDTO response = restTemplate.getForObject(url, ProductClientDTO.class);
            if (response == null || response.getData() == null || !response.getData().isActive()) {
                throw new ResourceNotFoundException("Product with ID " + productId + " does not exist or is inactive");
            }
            return response.getData();
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Product ID {} not found in Product Service", productId);
            throw new ResourceNotFoundException("Product with ID " + productId + " does not exist or is inactive");
        } catch (HttpClientErrorException ex) {
            log.warn("Client error querying Product Service: {}", ex.getMessage());
            throw new ResourceNotFoundException("Product with ID " + productId + " could not be retrieved");
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            log.error("Product Service communication failure: {}", ex.getMessage());
            throw new ProductServiceUnavailableException("Product Service is temporarily unavailable. Please retry shortly.", ex);
        }
    }
}

