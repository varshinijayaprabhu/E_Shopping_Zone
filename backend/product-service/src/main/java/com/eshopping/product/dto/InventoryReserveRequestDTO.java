package com.eshopping.product.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class InventoryReserveRequestDTO {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotEmpty(message = "Items to reserve cannot be empty")
    @Valid
    private List<ItemReservation> items;

    public InventoryReserveRequestDTO() {
    }

    public InventoryReserveRequestDTO(String orderNumber, List<ItemReservation> items) {
        this.orderNumber = orderNumber;
        this.items = items;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<ItemReservation> getItems() {
        return items;
    }

    public void setItems(List<ItemReservation> items) {
        this.items = items;
    }

    public static class ItemReservation {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @Min(value = 1, message = "Reservation quantity must be at least 1")
        private int quantity;

        public ItemReservation() {
        }

        public ItemReservation(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}

