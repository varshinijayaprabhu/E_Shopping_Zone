package com.eshopping.wallet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("E-ShoppingZone - Digital Wallet Microservice API")
                        .version("1.0.0")
                        .description("RESTful API documentation for E-ShoppingZone Digital Wallet Service. " +
                                "Manages customer digital balances, immutable transaction ledgers, " +
                                "concurrency-safe deductions via pessimistic locking, and idempotent Saga payments and refunds.")
                        .contact(new Contact()
                                .name("E-ShoppingZone Engineering Team")
                                .email("engineering@eshoppingzone.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:" + serverPort).description("Direct Microservice Server"),
                        new Server().url("http://localhost:8080").description("API Gateway Routing")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token issued by Profile Service")));
    }
}

