package com.eshopping.admin.dto;

import java.math.BigDecimal;

public class OrderMetricsSummaryDTO {

    private long totalOrders;
    private BigDecimal totalRevenue;

    public OrderMetricsSummaryDTO() {
    }

    public OrderMetricsSummaryDTO(long totalOrders, BigDecimal totalRevenue) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
