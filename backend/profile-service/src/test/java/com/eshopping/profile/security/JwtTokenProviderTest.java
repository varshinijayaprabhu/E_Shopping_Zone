package com.eshopping.profile.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 900000L); // 15 mins
        tokenProvider.init();
    }

    @Test
    @DisplayName("Generate and validate valid JWT access token")
    void testGenerateAndValidateToken() {
        Set<String> roles = new HashSet<>(Collections.singletonList("ROLE_CUSTOMER"));
        String token = tokenProvider.generateAccessToken(100L, "test@eshopping.com", "Test User", roles);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(100L, tokenProvider.getUserIdFromToken(token));
        assertEquals("test@eshopping.com", tokenProvider.getEmailFromToken(token));

        List<String> extractedRoles = tokenProvider.getRolesFromToken(token);
        assertTrue(extractedRoles.contains("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Invalid token validation fails cleanly")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }
}

