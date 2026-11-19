package com.eshopping.wallet.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    @DisplayName("CustomUserDetails getters and boolean methods return expected values")
    void testCustomUserDetails() {
        CustomUserDetails user = new CustomUserDetails(100L, "customer@example.com", Collections.singleton("CUSTOMER"));

        assertEquals(100L, user.getId());
        assertEquals("customer@example.com", user.getEmail());
        assertEquals("customer@example.com", user.getUsername());
        assertNull(user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        assertFalse(user.getAuthorities().isEmpty());
        assertEquals("ROLE_CUSTOMER", user.getAuthorities().iterator().next().getAuthority());
    }
}
