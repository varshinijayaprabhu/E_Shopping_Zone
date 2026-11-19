package com.eshopping.profile.security;

import com.eshopping.profile.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    @DisplayName("CustomUserDetails delegates properly to User entity")
    void testCustomUserDetails() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed-pw");
        user.setFullName("John Doe");
        user.setEnabled(true);
        user.setRoles(Collections.singleton("ROLE_CUSTOMER"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertEquals(5L, userDetails.getId());
        assertEquals("user@example.com", userDetails.getEmail());
        assertEquals("user@example.com", userDetails.getUsername());
        assertEquals("hashed-pw", userDetails.getPassword());
        assertEquals("John Doe", userDetails.getFullName());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }
}
