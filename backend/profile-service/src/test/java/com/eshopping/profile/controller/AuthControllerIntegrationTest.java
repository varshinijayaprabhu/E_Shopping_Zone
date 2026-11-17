package com.eshopping.profile.controller;

import com.eshopping.profile.dto.*;
import com.eshopping.profile.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/auth/register: Valid request returns 201 Created")
    void testRegisterCustomer_Valid() throws Exception {
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "john.doe@example.com", "Password@123", "John Doe", "+91-9876543210"
        );

        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                1L, "john.doe@example.com", "John Doe", Collections.singleton("ROLE_CUSTOMER")
        );
        AuthResponseDTO responseDTO = new AuthResponseDTO("mock_jwt", 900L, summary);

        when(authService.registerCustomer(any(), any(), any(), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock_jwt"))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register: Invalid email returns 400 Bad Request with field error")
    void testRegisterCustomer_InvalidEmail() throws Exception {
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "invalid-email", "Password@123", "John Doe", "+91-9876543210"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("POST /api/auth/login: Valid credentials returns 200 OK")
    void testLogin_Valid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("john.doe@example.com", "Password@123");

        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                1L, "john.doe@example.com", "John Doe", Collections.singleton("ROLE_CUSTOMER")
        );
        AuthResponseDTO responseDTO = new AuthResponseDTO("mock_jwt_logged_in", 900L, summary);

        when(authService.login(any(), any(), any(), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock_jwt_logged_in"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh: Missing Anti-CSRF X-Requested-With header returns 403 Forbidden")
    void testRefresh_MissingCsrfHeader() throws Exception {
        Cookie refreshCookie = new Cookie("eshopping_refresh_token", "sample_refresh_token");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF_VIOLATION"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh: With Anti-CSRF header and cookie returns 200 OK")
    void testRefresh_ValidWithCsrfHeader() throws Exception {
        Cookie refreshCookie = new Cookie("eshopping_refresh_token", "sample_refresh_token");
        RefreshTokenResponseDTO responseDTO = new RefreshTokenResponseDTO("new_access_jwt", 900L);

        when(authService.refreshToken(eq("sample_refresh_token"), any(), any(), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access_jwt"));
    }

    @Test
    @DisplayName("POST /api/auth/oauth2/exchange: Valid code exchanges for access token and user summary")
    void testExchangeOAuth2Code_Valid() throws Exception {
        OAuth2ExchangeRequestDTO request = new OAuth2ExchangeRequestDTO("test_exchange_code");
        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                2L, "oauth_user@example.com", "OAuth User", Collections.singleton("ROLE_CUSTOMER")
        );
        AuthResponseDTO responseDTO = new AuthResponseDTO("exchanged_jwt_token", 900L, summary);

        when(authService.exchangeOAuth2Code(any(), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("exchanged_jwt_token"))
                .andExpect(jsonPath("$.user.email").value("oauth_user@example.com"));
    }
}

