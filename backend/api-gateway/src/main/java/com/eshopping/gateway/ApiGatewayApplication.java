package com.eshopping.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud API Gateway for E-ShoppingZone.
 * Single entry point for frontend and API clients. Handles dynamic routing,
 * load balancing via Eureka, CORS policies, and request correlation.
 *
 * Baseline: Java 8, Spring Boot 2.7.x, Spring Cloud 2021.0.x.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        log.info("==================================================================");
        log.info("  E-ShoppingZone API Gateway started on http://localhost:8080     ");
        log.info("==================================================================");
    }
}

