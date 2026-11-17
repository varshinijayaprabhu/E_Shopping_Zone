package com.eshopping.profile.security;

import com.eshopping.profile.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                "Full authentication is required to access this resource",
                request.getRequestURI(),
                request.getHeader("X-Correlation-Id")
        );

        response.getOutputStream().println(objectMapper.writeValueAsString(errorResponse));
    }
}

