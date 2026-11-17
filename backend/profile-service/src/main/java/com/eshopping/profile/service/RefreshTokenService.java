package com.eshopping.profile.service;

import com.eshopping.profile.entity.User;

public interface RefreshTokenService {

    String createRefreshToken(User user, String clientIp, String userAgent);

    TokenRotationResult rotateRefreshToken(String rawOldToken, String clientIp, String userAgent);

    void revokeRefreshToken(String rawToken);

    void revokeAllUserTokens(User user);

    class TokenRotationResult {
        private final String newRawRefreshToken;
        private final User user;

        public TokenRotationResult(String newRawRefreshToken, User user) {
            this.newRawRefreshToken = newRawRefreshToken;
            this.user = user;
        }

        public String getNewRawRefreshToken() {
            return newRawRefreshToken;
        }

        public User getUser() {
            return user;
        }
    }
}

