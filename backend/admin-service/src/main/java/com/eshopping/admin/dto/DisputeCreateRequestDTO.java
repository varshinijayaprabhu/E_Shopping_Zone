package com.eshopping.admin.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DisputeCreateRequestDTO {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Merchant ID is required")
    private Long merchantId;

    @NotBlank(message = "Issue reason is required")
    private String issueReason;

    private String customerComment;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    private BigDecimal refundAmount;

    public DisputeCreateRequestDTO() {
    }

    public DisputeCreateRequestDTO(Long orderId, Long merchantId, String issueReason,
                                  String customerComment, BigDecimal refundAmount) {
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.issueReason = issueReason;
        this.customerComment = customerComment;
        this.refundAmount = refundAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
}
