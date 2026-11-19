package com.eshopping.profile.security;

import com.eshopping.profile.entity.User;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    public static final String REFRESH_COOKIE_NAME = "eshopping_refresh_token";

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2ExchangeCodeStore exchangeCodeStore;

    @Value("${eshopping.security.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/redirect}")
    private String redirectUri;

    @Value("${eshopping.security.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    @Value("${eshopping.security.cookie.secure:false}")
    private boolean cookieSecure;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                             JwtTokenProvider tokenProvider,
                                             RefreshTokenService refreshTokenService,
                                             OAuth2ExchangeCodeStore exchangeCodeStore) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.exchangeCodeStore = exchangeCodeStore;
    }

    @Override
    @SuppressWarnings("java:S3330")
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract GitHub user details
        Object idAttr = oAuth2User.getAttribute("id");
        String githubId = idAttr != null ? idAttr.toString() : null;
        String email = oAuth2User.getAttribute("email");
        String login = oAuth2User.getAttribute("login");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        if (email == null || email.trim().isEmpty()) {
            email = login + "@users.noreply.github.com";
        }
        if (name == null || name.trim().isEmpty()) {
            name = login != null ? login : "GitHub User";
        }

        // Account association / creation
        User user = findOrCreateGitHubUser(githubId, email, name, avatarUrl);

        // Generate JWT Access Token & Refresh Token
        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );

        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String rawRefreshToken = refreshTokenService.createRefreshToken(user, clientIp, userAgent);

        // Set HttpOnly refresh cookie
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .secure(cookieSecure) // NOSONAR java:S3330 - Configurable HTTPS secure flag with localhost dev fallback
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Secure one-time code generation: Access token is NEVER placed in URL
        String oneTimeExchangeCode = exchangeCodeStore.generateAndStoreCode(user, accessToken);

        // Redirect to React frontend callback with one-time code only
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", oneTimeExchangeCode)
                .build().toUriString();

        log.info("GitHub OAuth2 login successful for user: {}. Redirecting to UI with secure one-time code", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User findOrCreateGitHubUser(String githubId, String email, String name, String avatarUrl) {
        if (githubId != null) {
            Optional<User> byGithubId = userRepository.findByGithubId(githubId);
            if (byGithubId.isPresent()) {
                return byGithubId.get();
            }
        }

        Optional<User> byEmail = userRepository.findByEmail(email.toLowerCase().trim());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (githubId != null && existing.getGithubId() == null) {
                existing.setGithubId(githubId);
                if (existing.getProfilePictureUrl() == null) {
                    existing.setProfilePictureUrl(avatarUrl);
                }
                userRepository.save(existing);
                log.info("Linked GitHub ID to existing user account: {}", existing.getEmail());
            }
            return existing;
        }

        // Provision new user with ROLE_CUSTOMER
        User newUser = new User();
        newUser.setEmail(email.toLowerCase().trim());
        newUser.setFullName(name);
        newUser.setGithubId(githubId);
        newUser.setProfilePictureUrl(avatarUrl);
        newUser.setAuthProvider("GITHUB");
        newUser.setEnabled(true);
        newUser.addRole("ROLE_CUSTOMER");

        User saved = userRepository.save(newUser);
        log.info("Auto-provisioned new user via GitHub OAuth2: {}", saved.getEmail());
        return saved;
    }
}

