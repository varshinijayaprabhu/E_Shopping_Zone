package com.eshopping.profile.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI profileOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-ShoppingZone - Profile Microservice API")
                        .description("Authentication, User & Merchant Profiles, Refresh Token Rotation, and GitHub OAuth 2.0")
                        .version("1.0.0")
                        .contact(new Contact().name("E-ShoppingZone Engineering").email("support@eshopping.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter valid JWT access token obtained from /api/auth/login")));
    }
}

