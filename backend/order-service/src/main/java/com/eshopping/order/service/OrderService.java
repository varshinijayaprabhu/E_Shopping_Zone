package com.eshopping.order.service;

import com.eshopping.order.dto.CheckoutRequestDTO;
import com.eshopping.order.dto.MerchantOrderItemDTO;
import com.eshopping.order.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderDTO checkout(CheckoutRequestDTO request, Long customerId, String authHeader);

    Page<OrderDTO> getCustomerOrders(Long customerId, Pageable pageable);

    OrderDTO getOrderById(Long orderId, Long customerId);

    OrderDTO getOrderByOrderNumber(String orderNumber);

    Page<MerchantOrderItemDTO> getMerchantOrders(Long merchantId, Pageable pageable);

    OrderDTO cancelOrder(Long orderId, Long customerId, String reason);

    int reconcilePendingSagas();
}

