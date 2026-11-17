package com.eshopping.profile.service.impl;

import com.eshopping.profile.entity.RefreshToken;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.InvalidTokenException;
import com.eshopping.profile.exception.TokenReplayException;
import com.eshopping.profile.repository.RefreshTokenRepository;
import com.eshopping.profile.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${eshopping.security.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public String createRefreshToken(User user, String clientIp, String userAgent) {
        String rawToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshToken.setRevoked(false);
        refreshToken.setClientIp(clientIp);
        refreshToken.setUserAgent(userAgent);

        refreshTokenRepository.save(refreshToken);
        log.debug("Created new refresh token for user: {}", user.getEmail());
        return rawToken;
    }

    @Override
    public TokenRotationResult rotateRefreshToken(String rawOldToken, String clientIp, String userAgent) {
        if (rawOldToken == null || rawOldToken.trim().isEmpty()) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        String oldTokenHash = hashToken(rawOldToken);
        // Concurrency defense: pessimistic write lock on the active token row to prevent race conditions during rotation
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashForUpdate(oldTokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        User user = existingToken.getUser();

        // Replay Attack Detection
        if (existingToken.isRevoked()) {
            log.error("🚨 REPLAY ATTACK DETECTED! Already-revoked refresh token used for user: {}. Revoking all sessions!", user.getEmail());
            refreshTokenRepository.revokeAllUserTokens(user);
            throw new TokenReplayException("Refresh token replay detected. All active sessions have been invalidated.");
        }

        // Expiration check
        if (existingToken.isExpired()) {
            log.warn("Expired refresh token presented for user: {}", user.getEmail());
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Revoke the old token
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Issue new rotated token with lineage tracking
        String newRawToken = generateSecureRandomToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setTokenHash(newTokenHash);
        newToken.setParentTokenHash(oldTokenHash);
        newToken.setExpiryDate(LocalDateTime.now().plusDays(refreshExpirationDays));
        newToken.setRevoked(false);
        newToken.setClientIp(clientIp);
        newToken.setUserAgent(userAgent);

        refreshTokenRepository.save(newToken);
        log.debug("Successfully rotated refresh token for user: {}", user.getEmail());

        return new TokenRotationResult(newRawToken, user);
    }

    @Override
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return;
        }
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.debug("Revoked refresh token for user: {}", token.getUser().getEmail());
        });
    }

    @Override
    public void revokeAllUserTokens(User user) {
        int count = refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Revoked all ({}) active refresh tokens for user: {}", count, user.getEmail());
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

