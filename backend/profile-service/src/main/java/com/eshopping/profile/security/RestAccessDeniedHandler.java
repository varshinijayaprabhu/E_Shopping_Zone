package com.eshopping.profile.security;

import com.eshopping.profile.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have the required authority or role to perform this action",
                request.getRequestURI(),
                request.getHeader("X-Correlation-Id")
        );

        response.getOutputStream().println(objectMapper.writeValueAsString(errorResponse));
    }
}

