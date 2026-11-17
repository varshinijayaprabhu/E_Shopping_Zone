package com.eshopping.product.dto;

import java.util.List;

public class InventoryReservationResponseDTO {

    private String orderNumber;
    private List<ReservedItemResult> reservedItems;

    public InventoryReservationResponseDTO() {
    }

    public InventoryReservationResponseDTO(String orderNumber, List<ReservedItemResult> reservedItems) {
        this.orderNumber = orderNumber;
        this.reservedItems = reservedItems;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<ReservedItemResult> getReservedItems() {
        return reservedItems;
    }

    public void setReservedItems(List<ReservedItemResult> reservedItems) {
        this.reservedItems = reservedItems;
    }

    public static class ReservedItemResult {
        private Long productId;
        private int quantity;
        private int remainingStock;

        public ReservedItemResult() {
        }

        public ReservedItemResult(Long productId, int quantity, int remainingStock) {
            this.productId = productId;
            this.quantity = quantity;
            this.remainingStock = remainingStock;
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

        public int getRemainingStock() {
            return remainingStock;
        }

        public void setRemainingStock(int remainingStock) {
            this.remainingStock = remainingStock;
        }
    }
}

