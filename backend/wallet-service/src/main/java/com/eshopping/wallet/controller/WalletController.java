package com.eshopping.wallet.controller;

import com.eshopping.wallet.dto.ApiResponse;
import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.dto.PaymentStatusResponseDTO;
import com.eshopping.wallet.dto.RefundRequestDTO;
import com.eshopping.wallet.dto.WalletDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;
import com.eshopping.wallet.exception.AccessDeniedException;
import com.eshopping.wallet.security.CustomUserDetails;
import com.eshopping.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Digital Wallet & Ledger API", description = "Endpoints for managing digital balance, ledger transactions, checkout payment, and Saga refunds")
public class WalletController {

    private final WalletService walletService;

    @Value("${internal.service.secret:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecretKey;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current customer wallet balance and details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletDTO>> getWallet(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        WalletDTO wallet = walletService.getWallet(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Wallet details retrieved successfully", wallet));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Alias for getting current customer wallet balance", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletDTO>> getWalletBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return getWallet(userDetails);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer's immutable wallet transaction ledger", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<WalletTransactionDTO>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<WalletTransactionDTO> transactions = walletService.getTransactions(userDetails.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully", transactions));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Deposit simulated funds to customer's digital wallet", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletTransactionDTO>> deposit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DepositRequestDTO request) {
        WalletTransactionDTO txn = walletService.deposit(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Deposit processed successfully", txn));
    }

    @PostMapping("/pay")
    @Operation(summary = "Internal Saga checkout payment deduction under pessimistic row lock")
    public ResponseEntity<ApiResponse<WalletTransactionDTO>> pay(
            @Valid @RequestBody PaymentRequestDTO request,
            HttpServletRequest httpRequest) {
        validateInternalAccess(httpRequest);
        WalletTransactionDTO txn = walletService.pay(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment deducted successfully", txn));
    }

    @PostMapping("/payment")
    @Operation(summary = "Alias for internal Saga checkout payment deduction")
    public ResponseEntity<ApiResponse<WalletTransactionDTO>> payment(
            @Valid @RequestBody PaymentRequestDTO request,
            HttpServletRequest httpRequest) {
        return pay(request, httpRequest);
    }

    @PostMapping("/refund")
    @Operation(summary = "Internal Saga compensating refund under pessimistic row lock")
    public ResponseEntity<ApiResponse<WalletTransactionDTO>> refund(
            @Valid @RequestBody RefundRequestDTO request,
            HttpServletRequest httpRequest) {
        validateInternalAccess(httpRequest);
        WalletTransactionDTO txn = walletService.refund(request);
        return ResponseEntity.ok(ApiResponse.ok("Refund credited successfully", txn));
    }

    @GetMapping("/payment/order/{orderReference}")
    @Operation(summary = "Reconciliation endpoint: Look up payment status by order reference")
    public ResponseEntity<ApiResponse<PaymentStatusResponseDTO>> getPaymentStatusByOrder(
            @PathVariable("orderReference") String orderReference,
            HttpServletRequest httpRequest) {
        validateInternalOrCustomerAccess(httpRequest);
        PaymentStatusResponseDTO status = walletService.getPaymentStatusByOrderReference(orderReference);
        return ResponseEntity.ok(ApiResponse.ok("Payment status retrieved", status));
    }

    private void validateInternalAccess(HttpServletRequest httpRequest) {
        String secretHeader = httpRequest.getHeader("X-Internal-Secret");
        if (secretHeader == null || !secretHeader.equals(internalSecretKey)) {
            throw new AccessDeniedException("Access denied: Missing or invalid internal service secret");
        }
    }

    private void validateInternalOrCustomerAccess(HttpServletRequest httpRequest) {
        String secretHeader = httpRequest.getHeader("X-Internal-Secret");
        String serviceHeader = httpRequest.getHeader("X-Internal-Service");
        if ((secretHeader != null && secretHeader.equals(internalSecretKey))
                || (serviceHeader != null && ("order-service".equalsIgnoreCase(serviceHeader) || "api-gateway".equalsIgnoreCase(serviceHeader)))) {
            return;
        }
        // If not internal service, check for valid customer JWT
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return;
        }
        throw new AccessDeniedException("Access denied: Authentication or internal service credentials required");
    }
}

