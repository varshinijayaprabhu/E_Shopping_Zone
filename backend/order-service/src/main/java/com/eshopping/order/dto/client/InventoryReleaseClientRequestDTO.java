package com.eshopping.order.dto.client;

import java.util.List;

public class InventoryReleaseClientRequestDTO {

    private String orderNumber;
    private List<ItemRelease> items;

    public InventoryReleaseClientRequestDTO() {
    }

    public InventoryReleaseClientRequestDTO(String orderNumber, List<ItemRelease> items) {
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
        private Long productId;
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

