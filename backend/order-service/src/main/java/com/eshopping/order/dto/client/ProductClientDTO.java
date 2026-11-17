package com.eshopping.order.dto.client;

import java.math.BigDecimal;

public class ProductClientDTO {

    private boolean success;
    private String message;
    private ProductData data;

    public ProductClientDTO() {
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

    public ProductData getData() {
        return data;
    }

    public void setData(ProductData data) {
        this.data = data;
    }

    public static class ProductData {
        private Long id;
        private Long merchantId;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String sku;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private int availableStock;
        private int reservedStock;
        private boolean active;

        public ProductData() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(Long merchantId) {
            this.merchantId = merchantId;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public int getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(int availableStock) {
            this.availableStock = availableStock;
        }

        public int getReservedStock() {
            return reservedStock;
        }

        public void setReservedStock(int reservedStock) {
            this.reservedStock = reservedStock;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}

