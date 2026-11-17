package com.eshopping.admin.controller;

import com.eshopping.admin.dto.*;
import com.eshopping.admin.security.CustomUserDetails;
import com.eshopping.admin.service.AdminGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Dispute Management", description = "Endpoints for raising, tracking, and responding to order dispute tickets with SLA countdowns")
public class DisputeController {

    private final AdminGovernanceService adminGovernanceService;

    public DisputeController(AdminGovernanceService adminGovernanceService) {
        this.adminGovernanceService = adminGovernanceService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Raise Order Dispute Ticket", description = "Initiates a dispute ticket with a 2-minute merchant SLA timer")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> createDispute(
            @Valid @RequestBody DisputeCreateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        DisputeResponseDTO created = adminGovernanceService.createDispute(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dispute raised successfully. Merchant has 2 minutes to respond.", created));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get My Disputes", description = "Returns disputes associated with the authenticated customer or merchant")
    public ResponseEntity<ApiResponse<List<DisputeResponseDTO>>> getMyDisputes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        boolean isMerchant = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT"));

        List<DisputeResponseDTO> disputes = isMerchant
                ? adminGovernanceService.getMerchantDisputes(userId)
                : adminGovernanceService.getCustomerDisputes(userId);

        return ResponseEntity.ok(ApiResponse.success("My disputes retrieved successfully", disputes));
    }

    @PostMapping("/{id}/merchant-response")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "Merchant Dispute Response", description = "Merchant accepts (triggers refund) or rejects (escalates to admin) the dispute")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> merchantRespond(
            @PathVariable Long id,
            @Valid @RequestBody DisputeMerchantResponseRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long merchantId = userDetails.getId();
        DisputeResponseDTO updated = adminGovernanceService.merchantRespond(merchantId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Dispute response submitted successfully", updated));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Dispute Ticket Details", description = "Returns details and live remaining SLA seconds for a dispute ticket")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> getDisputeById(@PathVariable Long id) {
        DisputeResponseDTO dispute = adminGovernanceService.getDisputeById(id);
        return ResponseEntity.ok(ApiResponse.success("Dispute details retrieved successfully", dispute));
    }
}
