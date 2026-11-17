package com.eshopping.admin.client;

import com.eshopping.admin.dto.OrderMetricsSummaryDTO;

public interface OrderServiceClient {

    OrderMetricsSummaryDTO getMetricsSummary();

    void adminDisputeRefund(Long orderId, String reason);

    void updateOrderStatus(Long orderId, String status);
}
