package com.eshopping.wallet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid Bearer token sets authentication in SecurityContext")
    void testDoFilter_ValidBearerToken() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(100L);
        when(tokenProvider.getEmailFromToken(token)).thenReturn("user@eshopping.com");
        when(tokenProvider.getRolesFromToken(token)).thenReturn(Collections.singletonList("ROLE_CUSTOMER"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        assertEquals(100L, details.getId());
        assertEquals("user@eshopping.com", details.getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Gateway headers fallback sets authentication when no valid token")
    void testDoFilter_GatewayHeadersFallback() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("200");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_MERCHANT");
        when(request.getHeader("X-User-Email")).thenReturn("merchant@eshopping.com");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        assertEquals(200L, details.getId());
        assertEquals("merchant@eshopping.com", details.getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Gateway headers fallback with null email uses default gateway-user")
    void testDoFilter_GatewayHeadersFallback_NullEmail() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("300");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_CUSTOMER");
        when(request.getHeader("X-User-Email")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        assertEquals(300L, details.getId());
        assertEquals("gateway-user", details.getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("No auth headers passes cleanly through filter chain")
    void testDoFilter_NoHeaders() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Exception during token extraction is handled without breaking chain")
    void testDoFilter_ExceptionHandledCleanly() throws Exception {
        when(request.getHeader("Authorization")).thenThrow(new RuntimeException("Simulated error"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
