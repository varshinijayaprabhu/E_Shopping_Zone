package com.eshopping.profile.service;

import com.eshopping.profile.entity.RefreshToken;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.InvalidTokenException;
import com.eshopping.profile.exception.TokenReplayException;
import com.eshopping.profile.repository.RefreshTokenRepository;
import com.eshopping.profile.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationDays", 7);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("alice@eshopping.com");
        testUser.setFullName("Alice Customer");
    }

    @Test
    @DisplayName("Create refresh token persists SHA-256 hash in DB and returns raw token")
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(testUser, "127.0.0.1", "Mozilla");

        assertNotNull(rawToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Rotate valid refresh token revokes old and creates new with parent lineage")
    void testRotateRefreshToken_Success() {
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        tokenEntity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(tokenEntity));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.TokenRotationResult result = refreshTokenService.rotateRefreshToken(
                "raw_valid_token_string_here", "127.0.0.1", "Mozilla"
        );

        assertNotNull(result);
        assertNotNull(result.getNewRawRefreshToken());
        assertEquals(testUser, result.getUser());
        assertTrue(tokenEntity.isRevoked(), "Old token must be marked as revoked");
    }

    @Test
    @DisplayName("Replay attack: Rotating an already-revoked token triggers immediate revocation of all user sessions")
    void testRotateRefreshToken_ReplayAttackDetection() {
        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setUser(testUser);
        revokedToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        revokedToken.setRevoked(true); // Already revoked!

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(revokedToken));

        assertThrows(TokenReplayException.class, () ->
                refreshTokenService.rotateRefreshToken("replayed_token_string", "127.0.0.1", "Mozilla")
        );

        // Verify that all active tokens for this user were revoked!
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("Rotate expired refresh token throws InvalidTokenException")
    void testRotateRefreshToken_Expired() {
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // Expired yesterday
        expiredToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.rotateRefreshToken("expired_token", "127.0.0.1", "Mozilla")
        );
    }
}

