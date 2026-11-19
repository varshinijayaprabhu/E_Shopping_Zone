package com.eshopping.order.controller;

import com.eshopping.order.dto.ApiResponse;
import com.eshopping.order.dto.CancelOrderRequestDTO;
import com.eshopping.order.dto.CheckoutRequestDTO;
import com.eshopping.order.dto.MerchantOrderItemDTO;
import com.eshopping.order.dto.OrderDTO;
import com.eshopping.order.dto.OrderMetricsSummaryDTO;
import com.eshopping.order.exception.AccessDeniedException;
import com.eshopping.order.security.CustomUserDetails;
import com.eshopping.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order & Checkout Management", description = "Endpoints for checkout state-machine saga, customer orders, and merchant visibility")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final com.eshopping.order.service.RazorpayService razorpayService;

    @Value("${eshopping.internal.secret-key:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecretKey;

    public OrderController(OrderService orderService, com.eshopping.order.service.RazorpayService razorpayService) {
        this.orderService = orderService;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Initiate Checkout State-Machine Saga", description = "Reserves inventory, deducts wallet or confirms COD, persists order snapshot, and clears cart with automatic forward/compensating transactions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created and confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request, idempotency conflict, or empty cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "Payment failed or insufficient wallet balance"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient inventory stock")
    })
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(
            @Valid @RequestBody CheckoutRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long customerId = userDetails.getId();
        String authHeader = httpRequest.getHeader("Authorization");

        log.info("Received checkout request from customer {}", customerId);
        OrderDTO order = orderService.checkout(request, customerId, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Get Customer Order History", description = "Returns paginated orders placed by the authenticated customer with order line snapshots")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getCustomerOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Long customerId = userDetails.getId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<OrderDTO> orders = orderService.getCustomerOrders(customerId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Customer orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Get Order Details", description = "Fetches a specific order by ID with anti-IDOR customer isolation")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        OrderDTO order = orderService.getOrderById(id, customerId);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", order));
    }

    @GetMapping("/status/{orderNumber}")
    @Operation(summary = "Check Order Status by Order Number", description = "Public / authenticated lookup of order state and delivery status")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderDTO order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", order));
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "Get Merchant Order Items", description = "Returns paginated order line items containing products owned by the authenticated merchant")
    public ResponseEntity<ApiResponse<Page<MerchantOrderItemDTO>>> getMerchantOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Long merchantId = userDetails.getId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<MerchantOrderItemDTO> merchantItems = orderService.getMerchantOrders(merchantId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Merchant order items retrieved successfully", merchantItems));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Cancel Order", description = "Cancels an order, releases reserved stock, and refunds wallet balance if applicable")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        String reason = (request != null && StringUtils.hasText(request.getReason()))
                ? request.getReason()
                : "Customer requested cancellation";

        OrderDTO cancelledOrder = orderService.cancelOrder(id, customerId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelledOrder));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Internal Saga Reconciliation", description = "System endpoint to reconcile pending or stuck saga orders")
    public ResponseEntity<ApiResponse<Integer>> reconcilePendingSagas(HttpServletRequest httpRequest) {
        validateInternalAccess(httpRequest);
        int reconciledCount = orderService.reconcilePendingSagas();
        return ResponseEntity.ok(ApiResponse.success("Reconciliation executed", reconciledCount));
    }

    @GetMapping("/internal/metrics/summary")
    @Operation(summary = "Internal: Order metrics summary", description = "Returns total order count and total platform revenue")
    public ResponseEntity<ApiResponse<OrderMetricsSummaryDTO>> getMetricsSummary() {
        return ResponseEntity.ok(ApiResponse.success("Order metrics summary retrieved", orderService.getMetricsSummary()));
    }

    @PutMapping("/internal/{id}/status")
    @Operation(summary = "Internal: Update order status", description = "Updates status of an order")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated", orderService.updateOrderStatus(id, status)));
    }

    @PutMapping("/status/{orderNumber}")
    @Operation(summary = "Update Order Status by Order Number", description = "Fulfillment update (e.g. SHIPPED, DELIVERED)")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatusByNumber(
            @PathVariable String orderNumber,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated", orderService.updateOrderStatusByNumber(orderNumber, status)));
    }

    @PostMapping("/internal/{id}/dispute-refund")
    @Operation(summary = "Internal: Process administrative dispute refund", description = "Triggers refund compensation to customer wallet and marks status as REFUNDED_BY_ADMIN")
    public ResponseEntity<ApiResponse<OrderDTO>> adminDisputeRefund(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Dispute refund processed successfully", orderService.adminDisputeRefund(id, reason)));
    }

    @PostMapping("/payment/razorpay/create-order")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Create Razorpay Order Intent", description = "Computes cart total and returns a Razorpay Order ID for client checkout popup")
    public ResponseEntity<ApiResponse<com.eshopping.order.dto.RazorpayCreateOrderResponseDTO>> createRazorpayOrder(
            @Valid @RequestBody com.eshopping.order.dto.RazorpayCreateOrderRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long customerId = userDetails.getId();
        com.eshopping.order.dto.RazorpayCreateOrderResponseDTO res = razorpayService.createOrder(customerId, request, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Razorpay order intent created successfully", res));
    }

    @PostMapping("/payment/razorpay/verify")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Verify Razorpay Payment Signature & Complete Saga", description = "Verifies HMAC-SHA256 signature, reserves inventory stock, clears cart, and finalizes order")
    public ResponseEntity<ApiResponse<OrderDTO>> verifyRazorpayPayment(
            @Valid @RequestBody com.eshopping.order.dto.RazorpayVerifyPaymentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long customerId = userDetails.getId();
        OrderDTO order = razorpayService.verifyAndCompletePayment(customerId, request, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and order placed successfully", order));
    }

    private void validateInternalAccess(HttpServletRequest request) {
        String secret = request.getHeader("X-Internal-Secret");
        if (!StringUtils.hasText(secret) || !internalSecretKey.equals(secret)) {
            log.warn("Unauthorized attempt to access internal reconciliation endpoint from IP {}", request.getRemoteAddr());
            throw new AccessDeniedException("Access denied: missing or invalid internal service secret");
        }
    }
}

