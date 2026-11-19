package com.eshopping.profile.security;

import com.eshopping.profile.entity.User;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OAuth2ExchangeCodeStore exchangeCodeStore;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:5173/oauth2/redirect");
        ReflectionTestUtils.setField(handler, "refreshExpirationDays", 7);
        ReflectionTestUtils.setField(handler, "cookieSecure", false);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(response.encodeRedirectURL(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("onAuthenticationSuccess: existing user by githubId generates tokens and redirects")
    void testExistingGitHubUser() throws Exception {
        User existingUser = new User();
        existingUser.setId(10L);
        existingUser.setEmail("dev@github.com");
        existingUser.setFullName("Dev User");
        existingUser.setGithubId("gh-12345");
        existingUser.setRoles(Collections.singleton("ROLE_CUSTOMER"));

        when(oAuth2User.getAttribute("id")).thenReturn(12345);
        when(oAuth2User.getAttribute("email")).thenReturn("dev@github.com");
        when(oAuth2User.getAttribute("login")).thenReturn("devuser");
        when(oAuth2User.getAttribute("name")).thenReturn("Dev User");
        when(oAuth2User.getAttribute("avatar_url")).thenReturn("http://avatar.png");

        when(userRepository.findByGithubId("12345")).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateAccessToken(eq(10L), eq("dev@github.com"), eq("Dev User"), any()))
                .thenReturn("mock_access_jwt");
        when(refreshTokenService.createRefreshToken(eq(existingUser), any(), any()))
                .thenReturn("mock_raw_refresh");
        when(exchangeCodeStore.generateAndStoreCode(existingUser, "mock_access_jwt"))
                .thenReturn("code-uuid-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).addHeader(eq("Set-Cookie"), contains("eshopping_refresh_token=mock_raw_refresh"));
        verify(response).sendRedirect(contains("code=code-uuid-123"));
    }

    @Test
    @DisplayName("onAuthenticationSuccess: existing user by email links githubId and redirects")
    void testLinkExistingEmailUser() throws Exception {
        User existingUser = new User();
        existingUser.setId(15L);
        existingUser.setEmail("link@example.com");
        existingUser.setFullName("Link User");
        existingUser.setRoles(Collections.singleton("ROLE_CUSTOMER"));

        when(oAuth2User.getAttribute("id")).thenReturn(998877);
        when(oAuth2User.getAttribute("email")).thenReturn("link@example.com");
        when(oAuth2User.getAttribute("login")).thenReturn("linkuser");
        when(oAuth2User.getAttribute("name")).thenReturn("Link User");
        when(oAuth2User.getAttribute("avatar_url")).thenReturn("http://avatar.link");

        when(userRepository.findByGithubId("998877")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("link@example.com")).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateAccessToken(eq(15L), eq("link@example.com"), eq("Link User"), any()))
                .thenReturn("mock_jwt");
        when(refreshTokenService.createRefreshToken(eq(existingUser), any(), any()))
                .thenReturn("mock_refresh");
        when(exchangeCodeStore.generateAndStoreCode(existingUser, "mock_jwt"))
                .thenReturn("code-uuid-456");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("998877", existingUser.getGithubId());
        assertEquals("http://avatar.link", existingUser.getProfilePictureUrl());
        verify(userRepository).save(existingUser);
        verify(response).sendRedirect(contains("code=code-uuid-456"));
    }

    @Test
    @DisplayName("onAuthenticationSuccess: new user with missing email/name uses fallback and auto-provisions")
    void testNewUserFallbackAttributes() throws Exception {
        when(oAuth2User.getAttribute("id")).thenReturn(555);
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("login")).thenReturn("octocat");
        when(oAuth2User.getAttribute("name")).thenReturn(null);
        when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

        when(userRepository.findByGithubId("555")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("octocat@users.noreply.github.com")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(25L);
        savedUser.setEmail("octocat@users.noreply.github.com");
        savedUser.setFullName("octocat");
        savedUser.setRoles(Collections.singleton("ROLE_CUSTOMER"));

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("mock_jwt_new");
        when(refreshTokenService.createRefreshToken(any(), any(), any())).thenReturn("mock_refresh_new");
        when(exchangeCodeStore.generateAndStoreCode(any(), any())).thenReturn("code-uuid-new");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertEquals("octocat@users.noreply.github.com", created.getEmail());
        assertEquals("octocat", created.getFullName());
        assertTrue(created.getRoles().contains("ROLE_CUSTOMER"));
        assertEquals("GITHUB", created.getAuthProvider());
        verify(response).sendRedirect(contains("code=code-uuid-new"));
    }
}
