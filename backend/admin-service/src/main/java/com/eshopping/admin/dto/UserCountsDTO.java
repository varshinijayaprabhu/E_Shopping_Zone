package com.eshopping.admin.dto;

public class UserCountsDTO {

    private long totalUsers;
    private long activeCustomers;
    private long registeredMerchants;
    private long pendingMerchants;

    public UserCountsDTO() {
    }

    public UserCountsDTO(long totalUsers, long activeCustomers, long registeredMerchants, long pendingMerchants) {
        this.totalUsers = totalUsers;
        this.activeCustomers = activeCustomers;
        this.registeredMerchants = registeredMerchants;
        this.pendingMerchants = pendingMerchants;
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
}
