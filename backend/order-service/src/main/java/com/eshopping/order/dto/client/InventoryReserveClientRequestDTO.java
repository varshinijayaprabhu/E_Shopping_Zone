package com.eshopping.order.dto.client;

import java.util.List;

public class InventoryReserveClientRequestDTO {

    private String orderNumber;
    private List<ItemReservation> items;

    public InventoryReserveClientRequestDTO() {
    }

    public InventoryReserveClientRequestDTO(String orderNumber, List<ItemReservation> items) {
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
        private Long productId;
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

