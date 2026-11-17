package com.eshopping.product.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class InventoryReleaseRequestDTO {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotEmpty(message = "Items to release cannot be empty")
    @Valid
    private List<ItemRelease> items;

    public InventoryReleaseRequestDTO() {
    }

    public InventoryReleaseRequestDTO(String orderNumber, List<ItemRelease> items) {
        this.orderNumber = orderNumber;
        this.items = items;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<ItemRelease> getItems() {
        return items;
    }

    public void setItems(List<ItemRelease> items) {
        this.items = items;
    }

    public static class ItemRelease {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @Min(value = 1, message = "Release quantity must be at least 1")
        private int quantity;

        public ItemRelease() {
        }

        public ItemRelease(Long productId, int quantity) {
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

