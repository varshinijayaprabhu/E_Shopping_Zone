package com.eshopping.order.security;

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
    private final String secret = "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yZXNob3BwaW5nem9uZWp3dHRva2Vuc2VjdXJpdHkyMDI2";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Generate test token and validate successfully")
    void testGenerateAndValidateToken() {
        Set<String> roles = new HashSet<>(Collections.singletonList("ROLE_CUSTOMER"));
        String token = tokenProvider.generateTokenForTest(100L, "orderuser@eshopping.com", roles);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(100L, tokenProvider.getUserIdFromToken(token));
        assertEquals("orderuser@eshopping.com", tokenProvider.getEmailFromToken(token));

        List<String> extractedRoles = tokenProvider.getRolesFromToken(token);
        assertNotNull(extractedRoles);
        assertTrue(extractedRoles.contains("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Invalid token validation returns false gracefully")
    void testInvalidTokens() {
        assertFalse(tokenProvider.validateToken("invalid.token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken("Bearer "));
    }
}
