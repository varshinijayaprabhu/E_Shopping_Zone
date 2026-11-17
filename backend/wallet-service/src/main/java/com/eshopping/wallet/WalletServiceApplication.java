package com.eshopping.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Digital Wallet Microservice for E-ShoppingZone.
 *
 * Responsibilities:
 * - Customer digital wallet balance management
 * - Immutable financial transaction ledger
 * - Pessimistic write-locking for race-condition-free deductions
 * - Idempotent payments and refunds for Order Service Saga orchestration
 * - Reconciliation status lookup by order reference
 */
@SpringBootApplication
@EnableEurekaClient
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}

