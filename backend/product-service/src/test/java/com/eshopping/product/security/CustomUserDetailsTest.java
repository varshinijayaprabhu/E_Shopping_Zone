package com.eshopping.product.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    @DisplayName("CustomUserDetails getters and boolean methods return expected values")
    void testCustomUserDetails() {
        CustomUserDetails user = new CustomUserDetails(10L, "merchant@example.com", Collections.singleton("MERCHANT"));

        assertEquals(10L, user.getId());
        assertEquals("merchant@example.com", user.getEmail());
        assertEquals("merchant@example.com", user.getUsername());
        assertNull(user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        assertFalse(user.getAuthorities().isEmpty());
        assertTrue(user.getAuthorities().iterator().next().getAuthority().equals("ROLE_MERCHANT"));
    }
}
