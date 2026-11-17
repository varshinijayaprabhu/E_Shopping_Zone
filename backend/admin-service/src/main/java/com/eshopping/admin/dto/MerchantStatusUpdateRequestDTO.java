package com.eshopping.admin.dto;

import javax.validation.constraints.NotNull;

public class MerchantStatusUpdateRequestDTO {

    @NotNull(message = "Enabled status must not be null")
    private Boolean enabled;

    private String reason;

    public MerchantStatusUpdateRequestDTO() {
    }

    public MerchantStatusUpdateRequestDTO(Boolean enabled, String reason) {
        this.enabled = enabled;
        this.reason = reason;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
