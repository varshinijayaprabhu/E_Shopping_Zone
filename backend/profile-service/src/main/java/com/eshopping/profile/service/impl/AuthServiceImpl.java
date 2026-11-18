package com.eshopping.profile.service.impl;

import com.eshopping.profile.dto.*;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.EmailAlreadyExistsException;
import com.eshopping.profile.exception.InvalidCredentialsException;
import com.eshopping.profile.exception.InvalidInviteCodeException;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.security.JwtTokenProvider;
import com.eshopping.profile.service.AuthService;
import com.eshopping.profile.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    public static final String REFRESH_COOKIE_NAME = "eshopping_refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final com.eshopping.profile.security.OAuth2ExchangeCodeStore exchangeCodeStore;

    @Value("${eshopping.security.merchant-invite-code:ESHOPPING_MERCHANT_INVITE_2026}")
    private String merchantInviteCode;

    @Value("${eshopping.security.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           RefreshTokenService refreshTokenService,
                           com.eshopping.profile.security.OAuth2ExchangeCodeStore exchangeCodeStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.exchangeCodeStore = exchangeCodeStore;
    }

    @Override
    public AuthResponseDTO registerCustomer(CustomerRegisterRequestDTO request, String clientIp, String userAgent, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("User with email '" + request.getEmail() + "' already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAuthProvider("LOCAL");
        user.setEnabled(true);
        // Strictly ROLE_CUSTOMER only
        user.addRole("ROLE_CUSTOMER");

        User savedUser = userRepository.save(user);
        log.info("Registered new customer account: {}", savedUser.getEmail());

        return createAuthResponse(savedUser, clientIp, userAgent, response);
    }

    @Override
    public AuthResponseDTO registerMerchant(MerchantRegisterRequestDTO request, String clientIp, String userAgent, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("User with email '" + request.getEmail() + "' already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAuthProvider("LOCAL");
        // Option A: Merchant accounts start as disabled (PENDING_APPROVAL) until verified by Administrator
        user.setEnabled(false);
        user.addRole("ROLE_MERCHANT");

        User savedUser = userRepository.save(user);
        log.info("Registered new merchant account (PENDING APPROVAL): {}", savedUser.getEmail());

        // Return user summary without active session tokens until approved
        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRoles()
        );
        return new AuthResponseDTO(null, 0L, summary);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request, String clientIp, String userAgent, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Your account is pending administrator approval or has been disabled. Please contact support.");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return createAuthResponse(user, clientIp, userAgent, response);
    }

    @Override
    public RefreshTokenResponseDTO refreshToken(String rawRefreshToken, String clientIp, String userAgent, HttpServletResponse response) {
        RefreshTokenService.TokenRotationResult result = refreshTokenService.rotateRefreshToken(rawRefreshToken, clientIp, userAgent);
        User user = result.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );

        setRefreshCookie(response, result.getNewRawRefreshToken(), Duration.ofDays(refreshExpirationDays));

        return new RefreshTokenResponseDTO(newAccessToken, jwtTokenProvider.getExpirationMs() / 1000);
    }

    @Override
    public AuthResponseDTO exchangeOAuth2Code(OAuth2ExchangeRequestDTO request, HttpServletResponse response) {
        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new com.eshopping.profile.exception.InvalidTokenException("OAuth2 exchange code is missing");
        }
        com.eshopping.profile.security.OAuth2ExchangeCodeStore.CodeEntry entry = exchangeCodeStore.consumeCode(request.getCode());
        if (entry == null) {
            throw new com.eshopping.profile.exception.InvalidTokenException("Invalid or expired OAuth2 exchange code");
        }
        User user = entry.getUser();
        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );
        return new AuthResponseDTO(entry.getAccessToken(), jwtTokenProvider.getExpirationMs() / 1000, summary);
    }

    @Override
    public void logout(String rawRefreshToken, HttpServletResponse response) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
        clearRefreshCookie(response);
    }

    @Override
    public void logoutAll(User user, HttpServletResponse response) {
        refreshTokenService.revokeAllUserTokens(user);
        clearRefreshCookie(response);
    }

    private AuthResponseDTO createAuthResponse(User user, String clientIp, String userAgent, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );

        String rawRefreshToken = refreshTokenService.createRefreshToken(user, clientIp, userAgent);
        setRefreshCookie(response, rawRefreshToken, Duration.ofDays(refreshExpirationDays));

        AuthResponseDTO.UserSummary summary = new AuthResponseDTO.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles()
        );

        return new AuthResponseDTO(accessToken, jwtTokenProvider.getExpirationMs() / 1000, summary);
    }

    private void setRefreshCookie(HttpServletResponse response, String tokenValue, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokenValue)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(maxAge)
                .secure(false) // localhost development; true in production over HTTPS
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

