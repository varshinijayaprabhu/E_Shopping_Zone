package com.eshopping.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Server for E-ShoppingZone.
 * Maintains service registry and health status for all microservices.
 *
 * Baseline: Java 8, Spring Boot 2.7.x, Spring Cloud 2021.0.x.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
	
    private static final Logger log = LoggerFactory.getLogger(DiscoveryServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
        log.info("==================================================================");
        log.info("  E-ShoppingZone Discovery Server started on http://localhost:8761");
        log.info("==================================================================");
    }
}

