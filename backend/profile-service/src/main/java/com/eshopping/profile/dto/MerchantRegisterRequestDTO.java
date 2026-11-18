package com.eshopping.profile.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Merchant Registration Request DTO.
 * Requires an administrative merchant invite code for controlled provisioning.
 */
public class MerchantRegisterRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#^+\\-_])[A-Za-z\\d@$!%*?&.#^+\\-_]{8,64}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special symbol (@$!%*?&.#^+\\-_)"
    )
    private String password;

    @NotBlank(message = "Full name / business name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]{7,25}$", message = "Phone number must be a valid international or local phone format")
    private String phoneNumber;

    // Optional invite code (legacy/compatibility; no longer required for registration)
    private String merchantInviteCode;

    public MerchantRegisterRequestDTO() {
    }

    public MerchantRegisterRequestDTO(String email, String password, String fullName, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public MerchantRegisterRequestDTO(String email, String password, String fullName, String phoneNumber, String merchantInviteCode) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.merchantInviteCode = merchantInviteCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getMerchantInviteCode() {
        return merchantInviteCode;
    }

    public void setMerchantInviteCode(String merchantInviteCode) {
        this.merchantInviteCode = merchantInviteCode;
    }
}

