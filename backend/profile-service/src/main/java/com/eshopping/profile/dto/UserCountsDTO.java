package com.eshopping.profile.dto;

public class UserCountsDTO {

    private long totalCustomers;
    private long totalMerchants;

    public UserCountsDTO() {
    }

    public UserCountsDTO(long totalCustomers, long totalMerchants) {
        this.totalCustomers = totalCustomers;
        this.totalMerchants = totalMerchants;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalMerchants() {
        return totalMerchants;
    }

    public void setTotalMerchants(long totalMerchants) {
        this.totalMerchants = totalMerchants;
    }
}
