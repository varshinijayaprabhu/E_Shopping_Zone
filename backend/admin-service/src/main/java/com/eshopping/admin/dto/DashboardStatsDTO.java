package com.eshopping.admin.dto;

import java.math.BigDecimal;

public class DashboardStatsDTO {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private long totalUsers;
    private long activeCustomers;
    private long registeredMerchants;
    private long pendingMerchants;
    private long pendingDisputes;
    private long escalatedDisputes;
    private long resolvedDisputes;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(long totalOrders, BigDecimal totalRevenue, long totalUsers,
                             long activeCustomers, long registeredMerchants, long pendingMerchants,
                             long pendingDisputes, long escalatedDisputes, long resolvedDisputes) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.totalUsers = totalUsers;
        this.activeCustomers = activeCustomers;
        this.registeredMerchants = registeredMerchants;
        this.pendingMerchants = pendingMerchants;
        this.pendingDisputes = pendingDisputes;
        this.escalatedDisputes = escalatedDisputes;
        this.resolvedDisputes = resolvedDisputes;
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

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(long activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public long getRegisteredMerchants() {
        return registeredMerchants;
    }

    public void setRegisteredMerchants(long registeredMerchants) {
        this.registeredMerchants = registeredMerchants;
    }

    public long getPendingMerchants() {
        return pendingMerchants;
    }

    public void setPendingMerchants(long pendingMerchants) {
        this.pendingMerchants = pendingMerchants;
    }

    public long getPendingDisputes() {
        return pendingDisputes;
    }

    public void setPendingDisputes(long pendingDisputes) {
        this.pendingDisputes = pendingDisputes;
    }

    public long getEscalatedDisputes() {
        return escalatedDisputes;
    }

    public void setEscalatedDisputes(long escalatedDisputes) {
        this.escalatedDisputes = escalatedDisputes;
    }

    public long getResolvedDisputes() {
        return resolvedDisputes;
    }

    public void setResolvedDisputes(long resolvedDisputes) {
        this.resolvedDisputes = resolvedDisputes;
    }
}
