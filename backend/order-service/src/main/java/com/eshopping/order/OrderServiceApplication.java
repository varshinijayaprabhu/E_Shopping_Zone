package com.eshopping.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Order Microservice & Checkout State-Machine Saga Orchestrator for E-ShoppingZone.
 *
 * Responsibilities:
 * - Coordinates end-to-end checkout Saga across Cart, Product, and Wallet microservices.
 * - Enforces zero-trust customer identity and strict anti-IDOR merchant visibility.
 * - Snapshots historical item prices, titles, quantities, and merchant IDs for data immutability.
 * - Handles compensation (stock release, wallet refunds) and idempotency across all Saga steps.
 * - Provides background recovery and reconciliation for in-flight/uncertain orders.
 */
@SpringBootApplication
@EnableEurekaClient
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

