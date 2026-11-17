package com.eshopping.order.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CancelOrderRequestDTO {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
    private String reason;

    public CancelOrderRequestDTO() {
    }

    public CancelOrderRequestDTO(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

