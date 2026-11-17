package com.eshopping.profile.controller;

import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.security.CustomUserDetails;
import com.eshopping.profile.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails createMockUserDetails(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hashed_pw");
        user.setFullName("Alice Customer");
        user.setEnabled(true);
        user.setRoles(Collections.singleton("ROLE_CUSTOMER"));
        return new CustomUserDetails(user);
    }

    @Test
    @DisplayName("GET /api/profile: Unauthenticated request returns 401 Unauthorized")
    void testGetProfile_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/profile: Authenticated request returns user profile")
    void testGetProfile_Authenticated() throws Exception {
        CustomUserDetails userDetails = createMockUserDetails(1L, "alice@eshopping.com");

        UserProfileResponseDTO profile = new UserProfileResponseDTO();
        profile.setId(1L);
        profile.setEmail("alice@eshopping.com");
        profile.setFullName("Alice Customer");
        profile.setCity("Bengaluru");

        when(profileService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/profile")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@eshopping.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Customer"))
                .andExpect(jsonPath("$.city").value("Bengaluru"));
    }

    @Test
    @DisplayName("PUT /api/profile: Authenticated request updates profile successfully")
    void testUpdateProfile_Authenticated() throws Exception {
        CustomUserDetails userDetails = createMockUserDetails(1L, "alice@eshopping.com");

        UserProfileUpdateRequestDTO request = new UserProfileUpdateRequestDTO();
        request.setFullName("Alice Updated");
        request.setPhoneNumber("+91-9876543210");
        request.setCity("Bengaluru");

        UserProfileResponseDTO updated = new UserProfileResponseDTO();
        updated.setId(1L);
        updated.setEmail("alice@eshopping.com");
        updated.setFullName("Alice Updated");
        updated.setCity("Bengaluru");

        when(profileService.updateProfile(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/profile")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Updated"));
    }

    @Test
    @DisplayName("POST /api/profile: Authenticated request saves/updates profile successfully")
    void testSaveProfile_POST_Authenticated() throws Exception {
        CustomUserDetails userDetails = createMockUserDetails(1L, "alice@eshopping.com");

        UserProfileUpdateRequestDTO request = new UserProfileUpdateRequestDTO();
        request.setFullName("Alice Updated Via POST");
        request.setPhoneNumber("+91-9876543210");
        request.setCity("Bengaluru");

        UserProfileResponseDTO updated = new UserProfileResponseDTO();
        updated.setId(1L);
        updated.setEmail("alice@eshopping.com");
        updated.setFullName("Alice Updated Via POST");
        updated.setCity("Bengaluru");

        when(profileService.updateProfile(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(post("/api/profile")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Updated Via POST"));
    }
}

