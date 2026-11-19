package com.eshopping.order.service;

import com.eshopping.order.dto.OrderDTO;
import com.eshopping.order.dto.RazorpayCreateOrderRequestDTO;
import com.eshopping.order.dto.RazorpayCreateOrderResponseDTO;
import com.eshopping.order.dto.RazorpayVerifyPaymentRequestDTO;
import com.eshopping.order.entity.Order;

import java.math.BigDecimal;

public interface RazorpayService {

    RazorpayCreateOrderResponseDTO createOrder(Long customerId, RazorpayCreateOrderRequestDTO request, String authHeader);

    OrderDTO verifyAndCompletePayment(Long customerId, RazorpayVerifyPaymentRequestDTO request, String authHeader);

    boolean refundPayment(Order order, BigDecimal refundAmount, String reason);
}
