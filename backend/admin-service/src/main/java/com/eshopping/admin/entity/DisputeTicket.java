package com.eshopping.admin.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_tickets", indexes = {
        @Index(name = "idx_disputes_order", columnList = "order_id"),
        @Index(name = "idx_disputes_customer", columnList = "customer_id"),
        @Index(name = "idx_disputes_merchant", columnList = "merchant_id"),
        @Index(name = "idx_disputes_status", columnList = "status")
})
public class DisputeTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "issue_reason", nullable = false, length = 255)
    private String issueReason;

    @Column(name = "customer_comment", columnDefinition = "TEXT")
    private String customerComment;

    @Column(name = "merchant_response", columnDefinition = "TEXT")
    private String merchantResponse;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DisputeStatus status = DisputeStatus.PENDING_MERCHANT;

    @Column(name = "merchant_deadline")
    private LocalDateTime merchantDeadline;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DisputeTicket() {
    }

    public DisputeTicket(Long orderId, Long customerId, Long merchantId, String issueReason,
                         String customerComment, BigDecimal refundAmount, LocalDateTime merchantDeadline) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.merchantId = merchantId;
        this.issueReason = issueReason;
        this.customerComment = customerComment;
        this.refundAmount = refundAmount != null ? refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        this.merchantDeadline = merchantDeadline;
        this.status = DisputeStatus.PENDING_MERCHANT;
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
        this.refundAmount = refundAmount != null ? refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
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
