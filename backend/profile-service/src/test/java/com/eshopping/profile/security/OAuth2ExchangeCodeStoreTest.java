package com.eshopping.profile.security;

import com.eshopping.profile.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2ExchangeCodeStoreTest {

    private OAuth2ExchangeCodeStore codeStore;
    private User testUser;

    @BeforeEach
    void setUp() {
        codeStore = new OAuth2ExchangeCodeStore();
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("oauth.user@example.com");
        testUser.setFullName("OAuth User");
        testUser.setRoles(Collections.singleton("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("generateAndStoreCode creates single-use code that can be consumed once")
    void testGenerateAndConsumeCode() {
        String token = "jwt.token.value";
        String code = codeStore.generateAndStoreCode(testUser, token);

        assertNotNull(code);
        assertFalse(code.isEmpty());

        OAuth2ExchangeCodeStore.CodeEntry entry = codeStore.consumeCode(code);
        assertNotNull(entry);
        assertEquals(testUser, entry.getUser());
        assertEquals(token, entry.getAccessToken());
        assertNotNull(entry.getExpiresAt());

        // Single-use: consuming again should return null
        OAuth2ExchangeCodeStore.CodeEntry secondAttempt = codeStore.consumeCode(code);
        assertNull(secondAttempt);
    }

    @Test
    @DisplayName("consumeCode returns null for null or non-existent code")
    void testConsumeInvalidCodes() {
        assertNull(codeStore.consumeCode(null));
        assertNull(codeStore.consumeCode("non-existent-uuid"));
    }
}
