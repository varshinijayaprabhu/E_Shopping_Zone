package com.eshopping.profile.security;

import com.eshopping.profile.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Anti-CSRF verification filter for cookie-based authentication endpoints.
 * Requires custom header X-Requested-With: XMLHttpRequest to prevent cross-site form/script forgery.
 */
@Component
public class AntiCsrfFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AntiCsrfFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final List<String> PROTECTED_COOKIE_PATHS = Arrays.asList(
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/logout-all"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (PROTECTED_COOKIE_PATHS.contains(path)) {
            String requestedWith = request.getHeader("X-Requested-With");
            if (!"XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                log.warn("CSRF violation: Missing or invalid X-Requested-With header on endpoint: {}", path);
                sendCsrfError(response, request, "Cross-Site Request Forgery protection: Mandatory 'X-Requested-With: XMLHttpRequest' header is missing");
                return;
            }

            String origin = request.getHeader("Origin");
            if (origin != null && !origin.startsWith("http://localhost:5173") && !origin.startsWith("http://localhost:8080")) {
                log.warn("CSRF violation: Disallowed Origin header: {} on endpoint: {}", origin, path);
                sendCsrfError(response, request, "Cross-Site Request Forgery protection: Disallowed Origin: " + origin);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendCsrfError(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpServletResponse.SC_FORBIDDEN,
                "CSRF_VIOLATION",
                message,
                request.getRequestURI(),
                request.getHeader("X-Correlation-Id")
        );
        response.getOutputStream().println(objectMapper.writeValueAsString(error));
    }
}
