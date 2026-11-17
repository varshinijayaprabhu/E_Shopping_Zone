package com.eshopping.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Reactive CORS Configuration for Spring Cloud Gateway.
 * Configured specifically for React SPA running on http://localhost:5173.
 */
@Configuration
public class CorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        org.springframework.web.cors.CorsConfiguration corsConfig = new org.springframework.web.cors.CorsConfiguration();

        // Allowed Origin: React Frontend
        corsConfig.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
        corsConfig.setMaxAge(3600L);

        // Allowed HTTP Methods
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allowed Inbound Headers
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Correlation-Id"
        ));

        // Exposed Outbound Headers
        corsConfig.setExposedHeaders(Arrays.asList(
                "Set-Cookie",
                "X-Correlation-Id"
        ));

        // Mandatory for HttpOnly Refresh Token Cookies
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

