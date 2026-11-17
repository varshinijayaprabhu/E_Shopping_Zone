package com.eshopping.cart.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class UpdateCartItemRequestDTO {

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 99, message = "Quantity cannot exceed 99 per item")
    private int quantity;

    public UpdateCartItemRequestDTO() {
    }

    public UpdateCartItemRequestDTO(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

