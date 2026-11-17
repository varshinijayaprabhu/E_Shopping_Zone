package com.eshopping.product.dto;

import java.util.List;

public class InventoryReleaseResponseDTO {

    private String orderNumber;
    private List<ReleasedItemResult> releasedItems;

    public InventoryReleaseResponseDTO() {
    }

    public InventoryReleaseResponseDTO(String orderNumber, List<ReleasedItemResult> releasedItems) {
        this.orderNumber = orderNumber;
        this.releasedItems = releasedItems;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<ReleasedItemResult> getReleasedItems() {
        return releasedItems;
    }

    public void setReleasedItems(List<ReleasedItemResult> releasedItems) {
        this.releasedItems = releasedItems;
    }

    public static class ReleasedItemResult {
        private Long productId;
        private int quantity;
        private int availableStock;

        public ReleasedItemResult() {
        }

        public ReleasedItemResult(Long productId, int quantity, int availableStock) {
            this.productId = productId;
            this.quantity = quantity;
            this.availableStock = availableStock;
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

        public int getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(int availableStock) {
            this.availableStock = availableStock;
        }
    }
}

