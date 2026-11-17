package com.eshopping.order.dto;

import java.math.BigDecimal;

public class OrderItemDTO {

    private Long id;
    private Long productId;
    private String productNameSnapshot;
    private String productSkuSnapshot;
    private BigDecimal unitPriceSnapshot;
    private int quantity;
    private BigDecimal subtotal;
    private Long merchantId;

    public OrderItemDTO() {
    }

    public OrderItemDTO(Long id, Long productId, String productNameSnapshot, String productSkuSnapshot,
                        BigDecimal unitPriceSnapshot, int quantity, BigDecimal subtotal, Long merchantId) {
        this.id = id;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.productSkuSnapshot = productSkuSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.merchantId = merchantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getProductSkuSnapshot() {
        return productSkuSnapshot;
    }

    public void setProductSkuSnapshot(String productSkuSnapshot) {
        this.productSkuSnapshot = productSkuSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }
}

