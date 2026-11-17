package com.eshopping.admin.client.impl;

import com.eshopping.admin.client.ProfileServiceClient;
import com.eshopping.admin.dto.ApiResponse;
import com.eshopping.admin.dto.MerchantProfileResponseDTO;
import com.eshopping.admin.dto.UserCountsDTO;
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

import java.util.Collections;
import java.util.List;

@Service
public class ProfileServiceClientImpl implements ProfileServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${services.profile.url:http://profile-service}")
    private String profileServiceUrl;

    @Value("${internal.service.secret:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecret;

    public ProfileServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);
        return headers;
    }

    @Override
    public List<MerchantProfileResponseDTO> getAllMerchants() {
        String url = profileServiceUrl + "/api/profile/internal/merchants";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            ResponseEntity<ApiResponse<List<MerchantProfileResponseDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<MerchantProfileResponseDTO>>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("Failed to fetch merchants from profile-service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMerchantStatus(Long merchantId, boolean enabled) {
        String url = profileServiceUrl + "/api/profile/internal/users/" + merchantId + "/status?enabled=" + enabled;
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Successfully updated status of merchant {} to enabled={}", merchantId, enabled);
        } catch (Exception ex) {
            log.error("Failed to update merchant status in profile-service: {}", ex.getMessage());
            throw new BadRequestException("Failed to update merchant status: " + ex.getMessage());
        }
    }

    @Override
    public UserCountsDTO getUserCounts() {
        String url = profileServiceUrl + "/api/profile/internal/metrics/counts";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
            ResponseEntity<ApiResponse<UserCountsDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserCountsDTO>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return new UserCountsDTO(0, 0, 0, 0);
        } catch (Exception ex) {
            log.error("Failed to fetch user counts from profile-service: {}", ex.getMessage());
            return new UserCountsDTO(0, 0, 0, 0);
        }
    }
}
