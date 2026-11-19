package com.eshopping.wallet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Generate and validate valid JWT token")
    void testGenerateAndValidateToken() {
        List<String> roles = Collections.singletonList("ROLE_CUSTOMER");
        String token = tokenProvider.generateToken(100L, "walletuser@eshopping.com", roles);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(100L, tokenProvider.getUserIdFromToken(token));
        assertEquals("walletuser@eshopping.com", tokenProvider.getEmailFromToken(token));

        List<String> extractedRoles = tokenProvider.getRolesFromToken(token);
        assertNotNull(extractedRoles);
        assertTrue(extractedRoles.contains("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Invalid token validation fails cleanly for various edge cases")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken("Bearer "));
    }
}
