package com.eshopping.admin.dto;

import com.eshopping.admin.entity.DisputeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisputeResponseDTO {

    private Long id;
    private Long orderId;
    private Long customerId;
    private Long merchantId;
    private String issueReason;
    private String customerComment;
    private String merchantResponse;
    private String adminNotes;
    private BigDecimal refundAmount;
    private DisputeStatus status;
    private LocalDateTime merchantDeadline;
    private Long remainingSeconds;
    private LocalDateTime escalatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DisputeResponseDTO() {
    }

    public DisputeResponseDTO(Long id, Long orderId, Long customerId, Long merchantId,
                              String issueReason, String customerComment, String merchantResponse,
                              String adminNotes, BigDecimal refundAmount, DisputeStatus status,
                              LocalDateTime merchantDeadline, Long remainingSeconds,
                              LocalDateTime escalatedAt, LocalDateTime resolvedAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.merchantId = merchantId;
        this.issueReason = issueReason;
        this.customerComment = customerComment;
        this.merchantResponse = merchantResponse;
        this.adminNotes = adminNotes;
        this.refundAmount = refundAmount;
        this.status = status;
        this.merchantDeadline = merchantDeadline;
        this.remainingSeconds = remainingSeconds;
        this.escalatedAt = escalatedAt;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public void setIssueReason(String issueReason) {
        this.issueReason = issueReason;
    }

    public String getCustomerComment() {
        return customerComment;
    }

    public void setCustomerComment(String customerComment) {
        this.customerComment = customerComment;
    }

    public String getMerchantResponse() {
        return merchantResponse;
    }

    public void setMerchantResponse(String merchantResponse) {
        this.merchantResponse = merchantResponse;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public LocalDateTime getMerchantDeadline() {
        return merchantDeadline;
    }

    public void setMerchantDeadline(LocalDateTime merchantDeadline) {
        this.merchantDeadline = merchantDeadline;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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
}
