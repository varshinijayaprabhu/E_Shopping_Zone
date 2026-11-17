package com.eshopping.profile.dto;

import java.util.Set;

public class AuthResponseDTO {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn = 900L;
    private UserSummary user;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String accessToken, long expiresIn, UserSummary user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public static class UserSummary {
        private Long id;
        private String email;
        private String fullName;
        private Set<String> roles;

        public UserSummary() {
        }

        public UserSummary(Long id, String email, String fullName, Set<String> roles) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.roles = roles;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }
}

