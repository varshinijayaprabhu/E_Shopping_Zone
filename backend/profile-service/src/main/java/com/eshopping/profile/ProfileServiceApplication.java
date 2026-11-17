package com.eshopping.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Profile & Authentication Microservice for E-ShoppingZone.
 *
 * Baseline: Java 8, Spring Boot 2.7.x, Spring Cloud 2021.0.x.
 * Port: 8081
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ProfileServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
        log.info("==================================================================");
        log.info("  E-ShoppingZone Profile Service started on http://localhost:8081 ");
        log.info("  Swagger UI: http://localhost:8081/swagger-ui/index.html         ");
        log.info("==================================================================");
    }
}

