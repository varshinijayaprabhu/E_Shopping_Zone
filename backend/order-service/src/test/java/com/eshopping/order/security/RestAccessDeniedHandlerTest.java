package com.eshopping.order.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RestAccessDeniedHandlerTest {

    @Test
    @DisplayName("Handle method outputs 403 Forbidden with JSON payload")
    void testHandle_ForbiddenResponse() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Insufficient privileges");

        handler.handle(request, response, exception);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("ACCESS_DENIED"));
        assertTrue(response.getContentAsString().contains("You do not have permission to access this resource"));
    }
}
