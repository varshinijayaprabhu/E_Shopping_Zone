package com.eshopping.profile.service;

import com.eshopping.profile.dto.*;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.EmailAlreadyExistsException;
import com.eshopping.profile.exception.InvalidCredentialsException;
import com.eshopping.profile.exception.InvalidInviteCodeException;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.security.JwtTokenProvider;
import com.eshopping.profile.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private com.eshopping.profile.security.OAuth2ExchangeCodeStore exchangeCodeStore;

    @InjectMocks
    private AuthServiceImpl authService;

    private final String validInviteCode = "ESHOPPING_MERCHANT_INVITE_2026";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "merchantInviteCode", validInviteCode);
        ReflectionTestUtils.setField(authService, "refreshExpirationDays", 7);
    }

    @Test
    @DisplayName("Customer registration: Successfully registers with ROLE_CUSTOMER only")
    void testRegisterCustomer_Success() {
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "alice@eshopping.com", "Password@123", "Alice Customer", "+91-9876543210"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.existsByEmail("alice@eshopping.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(eq(10L), eq("alice@eshopping.com"), eq("Alice Customer"), any()))
                .thenReturn("mock_jwt_token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class), any(), any()))
                .thenReturn("mock_refresh_token");

        AuthResponseDTO result = authService.registerCustomer(request, "127.0.0.1", "Mozilla", response);

        assertNotNull(result);
        assertEquals("mock_jwt_token", result.getAccessToken());
        assertEquals("alice@eshopping.com", result.getUser().getEmail());
        assertTrue(result.getUser().getRoles().contains("ROLE_CUSTOMER"));
        assertFalse(result.getUser().getRoles().contains("ROLE_MERCHANT"), "Customer must not possess merchant role");
        assertNotNull(response.getHeader("Set-Cookie"), "Refresh token cookie must be set");
    }

    @Test
    @DisplayName("Customer registration: Duplicate email throws EmailAlreadyExistsException")
    void testRegisterCustomer_DuplicateEmail() {
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "existing@eshopping.com", "Password@123", "Alice", "+91-9876543210"
        );
        when(userRepository.existsByEmail("existing@eshopping.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () ->
                authService.registerCustomer(request, "127.0.0.1", "Mozilla", new MockHttpServletResponse())
        );
    }

    @Test
    @DisplayName("Merchant registration: Success with valid invite code provisions ROLE_MERCHANT")
    void testRegisterMerchant_Success() {
        MerchantRegisterRequestDTO request = new MerchantRegisterRequestDTO(
                "merchant@eshopping.com", "Password@123", "Bob Merchant", "+91-9876543211", validInviteCode
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.existsByEmail("merchant@eshopping.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(20L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(eq(20L), eq("merchant@eshopping.com"), eq("Bob Merchant"), any()))
                .thenReturn("mock_merchant_jwt");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class), any(), any()))
                .thenReturn("mock_merchant_refresh");

        AuthResponseDTO result = authService.registerMerchant(request, "127.0.0.1", "Mozilla", response);

        assertNotNull(result);
        assertTrue(result.getUser().getRoles().contains("ROLE_MERCHANT"), "Merchant role must be provisioned");
    }

    @Test
    @DisplayName("Merchant registration: Invalid invite code throws InvalidInviteCodeException")
    void testRegisterMerchant_InvalidInviteCode() {
        MerchantRegisterRequestDTO request = new MerchantRegisterRequestDTO(
                "merchant@eshopping.com", "Password@123", "Bob Merchant", "+91-9876543211", "WRONG_CODE"
        );

        assertThrows(InvalidInviteCodeException.class, () ->
                authService.registerMerchant(request, "127.0.0.1", "Mozilla", new MockHttpServletResponse())
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login: Valid credentials authenticate and return tokens")
    void testLogin_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@eshopping.com");
        user.setPasswordHash("hashed_pass");
        user.setFullName("Alice Customer");
        user.setEnabled(true);
        user.setRoles(Collections.singleton("ROLE_CUSTOMER"));

        when(userRepository.findByEmail("alice@eshopping.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("alice@eshopping.com"), eq("Alice Customer"), any()))
                .thenReturn("mock_jwt");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(), any(), any())).thenReturn("mock_rt");

        LoginRequestDTO request = new LoginRequestDTO("alice@eshopping.com", "Password@123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthResponseDTO result = authService.login(request, "127.0.0.1", "Mozilla", response);

        assertNotNull(result);
        assertEquals("mock_jwt", result.getAccessToken());
    }

    @Test
    @DisplayName("Login: Invalid password throws InvalidCredentialsException")
    void testLogin_WrongPassword() {
        User user = new User();
        user.setEmail("alice@eshopping.com");
        user.setPasswordHash("hashed_pass");
        user.setEnabled(true);

        when(userRepository.findByEmail("alice@eshopping.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_pass")).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("alice@eshopping.com", "WrongPassword");

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(request, "127.0.0.1", "Mozilla", new MockHttpServletResponse())
        );
    }

    @Test
    @DisplayName("OAuth2 Code Exchange: Valid code exchanges for access token and user summary")
    void testExchangeOAuth2Code_Success() {
        User user = new User();
        user.setId(5L);
        user.setEmail("githubber@example.com");
        user.setFullName("GitHub Dev");
        user.addRole("ROLE_CUSTOMER");

        com.eshopping.profile.security.OAuth2ExchangeCodeStore.CodeEntry entry =
                new com.eshopping.profile.security.OAuth2ExchangeCodeStore.CodeEntry(user, "github_access_jwt", java.time.Instant.now().plusSeconds(60));

        when(exchangeCodeStore.consumeCode("valid_code_123")).thenReturn(entry);
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);

        OAuth2ExchangeRequestDTO request = new OAuth2ExchangeRequestDTO("valid_code_123");
        AuthResponseDTO result = authService.exchangeOAuth2Code(request, new MockHttpServletResponse());

        assertNotNull(result);
        assertEquals("github_access_jwt", result.getAccessToken());
        assertEquals("githubber@example.com", result.getUser().getEmail());
    }

    @Test
    @DisplayName("OAuth2 Code Exchange: Expired or non-existent code throws InvalidTokenException")
    void testExchangeOAuth2Code_InvalidOrExpired() {
        when(exchangeCodeStore.consumeCode("invalid_code")).thenReturn(null);

        OAuth2ExchangeRequestDTO request = new OAuth2ExchangeRequestDTO("invalid_code");
        assertThrows(com.eshopping.profile.exception.InvalidTokenException.class, () ->
                authService.exchangeOAuth2Code(request, new MockHttpServletResponse())
        );
    }
}

