package com.eshopping.profile.security;

import com.eshopping.profile.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache for single-use, short-lived (60 seconds)
 * OAuth2 exchange codes. Prevents access token exposure in frontend redirect URLs.
 */
@Component
public class OAuth2ExchangeCodeStore {

    private static final long CODE_TTL_SECONDS = 60L;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    public String generateAndStoreCode(User user, String accessToken) {
        // Periodic purge of expired entries
        purgeExpiredCodes();

        String code = UUID.randomUUID().toString();
        codeStore.put(code, new CodeEntry(user, accessToken, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        return code;
    }

    public CodeEntry consumeCode(String code) {
        if (code == null) {
            return null;
        }
        CodeEntry entry = codeStore.remove(code);
        if (entry != null && Instant.now().isBefore(entry.getExpiresAt())) {
            return entry;
        }
        return null;
    }

    private void purgeExpiredCodes() {
        Instant now = Instant.now();
        codeStore.entrySet().removeIf(e -> now.isAfter(e.getValue().getExpiresAt()));
    }

    public static class CodeEntry {
        private final User user;
        private final String accessToken;
        private final Instant expiresAt;

        public CodeEntry(User user, String accessToken, Instant expiresAt) {
            this.user = user;
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        public User getUser() {
            return user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}

