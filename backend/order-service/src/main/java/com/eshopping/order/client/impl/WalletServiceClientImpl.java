package com.eshopping.order.client.impl;

import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.client.WalletPayClientRequestDTO;
import com.eshopping.order.dto.client.WalletPaymentStatusClientResponseDTO;
import com.eshopping.order.dto.client.WalletRefundClientRequestDTO;
import com.eshopping.order.dto.client.WalletTransactionClientResponseDTO;
import com.eshopping.order.exception.PaymentFailedException;
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
public class WalletServiceClientImpl implements WalletServiceClient {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${services.wallet.url:http://wallet-service}")
    private String walletServiceUrl;

    @Value("${eshopping.internal.secret-key:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecretKey;

    public WalletServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public WalletTransactionClientResponseDTO pay(WalletPayClientRequestDTO request) {
        String url = walletServiceUrl + "/api/wallet/pay";
        HttpHeaders headers = createInternalHeaders();
        HttpEntity<WalletPayClientRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WalletTransactionClientResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    WalletTransactionClientResponseDTO.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody();
            }
            throw new PaymentFailedException("Payment processing failed: " +
                    (response.getBody() != null ? response.getBody().getMessage() : "Unknown wallet error"));
        } catch (HttpStatusCodeException ex) {
            log.error("Payment deduction failed for order {}: Status {} Body {}",
                    request.getOrderReference(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new PaymentFailedException("Payment deduction failed: " + ex.getResponseBodyAsString());
        } catch (PaymentFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error communicating with wallet service during payment for order {}: {}",
                    request.getOrderReference(), ex.getMessage());
            throw new PaymentFailedException("Wallet service connection timeout or failure: " + ex.getMessage());
        }
    }

    @Override
    public WalletTransactionClientResponseDTO refund(WalletRefundClientRequestDTO request) {
        String url = walletServiceUrl + "/api/wallet/refund";
        HttpHeaders headers = createInternalHeaders();
        HttpEntity<WalletRefundClientRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WalletTransactionClientResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    WalletTransactionClientResponseDTO.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody();
            }
            log.error("Compensating refund returned unsuccessful status for order {}: {}",
                    request.getOrderReference(), response.getBody() != null ? response.getBody().getMessage() : "null");
            return response.getBody();
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to issue compensating refund for order {}: {}",
                    request.getOrderReference(), ex.getMessage());
            return null;
        }
    }

    @Override
    public WalletPaymentStatusClientResponseDTO getPaymentStatusByOrder(String orderReference) {
        String url = walletServiceUrl + "/api/wallet/payment/order/" + orderReference;
        HttpHeaders headers = createInternalHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<WalletPaymentStatusClientResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    WalletPaymentStatusClientResponseDTO.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.warn("Payment status lookup failed for order {}: {}", orderReference, ex.getMessage());
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

