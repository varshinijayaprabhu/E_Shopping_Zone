package com.eshopping.order.dto.client;

import java.math.BigDecimal;

public class CartItemClientDTO {

    private Long itemId;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal unitPrice;
    private String imageUrl;
    private int quantity;
    private BigDecimal itemSubtotal;
    private boolean inStock;
    private int availableStock;

    public CartItemClientDTO() {
    }

    public CartItemClientDTO(Long itemId, Long productId, String productName, String productSku,
                             BigDecimal unitPrice, String imageUrl, int quantity,
                             BigDecimal itemSubtotal, boolean inStock, int availableStock) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.unitPrice = unitPrice;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.itemSubtotal = itemSubtotal;
        this.inStock = inStock;
        this.availableStock = availableStock;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getItemSubtotal() {
        return itemSubtotal;
    }

    public void setItemSubtotal(BigDecimal itemSubtotal) {
        this.itemSubtotal = itemSubtotal;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }
}

