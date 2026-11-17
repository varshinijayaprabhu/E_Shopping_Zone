package com.eshopping.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class MerchantProfileResponseDTO {

    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String city;
    private String state;
    private String country;
    private Set<String> roles;
    private boolean enabled;
    private LocalDateTime createdAt;

    public MerchantProfileResponseDTO() {
    }

    public MerchantProfileResponseDTO(Long id, String email, String fullName, String phoneNumber,
                                      String city, String state, String country, Set<String> roles,
                                      boolean enabled, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.state = state;
        this.country = country;
        this.roles = roles;
        this.enabled = enabled;
        this.createdAt = createdAt;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
