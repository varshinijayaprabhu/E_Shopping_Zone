package com.eshopping.order.dto;

import com.eshopping.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MerchantOrderItemDTO {

    private Long orderItemId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private LocalDateTime orderCreatedAt;
    private Long productId;
    private String productNameSnapshot;
    private String productSkuSnapshot;
    private BigDecimal unitPriceSnapshot;
    private int quantity;
    private BigDecimal subtotal;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;

    public MerchantOrderItemDTO() {
    }

    public MerchantOrderItemDTO(Long orderItemId, String orderNumber, OrderStatus orderStatus,
                                LocalDateTime orderCreatedAt, Long productId, String productNameSnapshot,
                                String productSkuSnapshot, BigDecimal unitPriceSnapshot, int quantity,
                                BigDecimal subtotal, String shippingName, String shippingPhone,
                                String shippingAddress) {
        this.orderItemId = orderItemId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.orderCreatedAt = orderCreatedAt;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.productSkuSnapshot = productSkuSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getOrderCreatedAt() {
        return orderCreatedAt;
    }

    public void setOrderCreatedAt(LocalDateTime orderCreatedAt) {
        this.orderCreatedAt = orderCreatedAt;
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

    public String getShippingName() {
        return shippingName;
    }

    public void setShippingName(String shippingName) {
        this.shippingName = shippingName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}

