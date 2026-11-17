package com.eshopping.order.dto.client;

import java.util.List;

public class InventoryReserveClientResponseDTO {

    private boolean success;
    private String message;
    private ReserveData data;

    public InventoryReserveClientResponseDTO() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReserveData getData() {
        return data;
    }

    public void setData(ReserveData data) {
        this.data = data;
    }

    public static class ReserveData {
        private String orderNumber;
        private List<ReservedItemResult> reservedItems;

        public ReserveData() {
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
    }

    public static class ReservedItemResult {
        private Long productId;
        private int quantity;
        private int remainingStock;

        public ReservedItemResult() {
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

