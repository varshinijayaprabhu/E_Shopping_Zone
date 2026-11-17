package com.eshopping.order.dto;

import com.eshopping.order.entity.OrderStatus;
import com.eshopping.order.entity.SagaState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDTO {

    private Long id;
    private String orderNumber;
    private Long customerId;
    private OrderStatus orderStatus;
    private SagaState sagaState;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String sagaFailureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items = new ArrayList<>();
    private PaymentRecordDTO payment;

    public OrderDTO() {
    }

    public OrderDTO(Long id, String orderNumber, Long customerId, OrderStatus orderStatus,
                    SagaState sagaState, BigDecimal subtotalAmount, BigDecimal shippingFee,
                    BigDecimal totalAmount, String shippingName, String shippingPhone,
                    String shippingAddress, String sagaFailureReason,
                    LocalDateTime createdAt, LocalDateTime updatedAt,
                    List<OrderItemDTO> items, PaymentRecordDTO payment) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.orderStatus = orderStatus;
        this.sagaState = sagaState;
        this.subtotalAmount = subtotalAmount;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.sagaFailureReason = sagaFailureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items != null ? items : new ArrayList<>();
        this.payment = payment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public SagaState getSagaState() {
        return sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getSagaFailureReason() {
        return sagaFailureReason;
    }

    public void setSagaFailureReason(String sagaFailureReason) {
        this.sagaFailureReason = sagaFailureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public PaymentRecordDTO getPayment() {
        return payment;
    }

    public void setPayment(PaymentRecordDTO payment) {
        this.payment = payment;
    }
}

