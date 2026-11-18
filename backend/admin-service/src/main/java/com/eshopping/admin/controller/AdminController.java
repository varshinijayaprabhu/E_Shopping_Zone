package com.eshopping.admin.controller;

import com.eshopping.admin.dto.*;
import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.security.CustomUserDetails;
import com.eshopping.admin.service.AdminGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Governance Management", description = "Endpoints for platform overview big numbers, merchant approvals/bans, dispute resolution, and audit logs")
public class AdminController {

    private final AdminGovernanceService adminGovernanceService;

    public AdminController(AdminGovernanceService adminGovernanceService) {
        this.adminGovernanceService = adminGovernanceService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Platform Summary Dashboard / Big Numbers", description = "Aggregates total orders, total revenue, active customers, registered merchants, and dispute counts")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", adminGovernanceService.getDashboardStats()));
    }

    @GetMapping("/merchants")
    @Operation(summary = "List all registered merchants", description = "Retrieves all merchant accounts with their approval and active status")
    public ResponseEntity<ApiResponse<List<MerchantProfileResponseDTO>>> getAllMerchants() {
        return ResponseEntity.ok(ApiResponse.success("Merchants retrieved successfully", adminGovernanceService.getAllMerchants()));
    }

    @PutMapping("/merchants/{id}/status")
    @Operation(summary = "Approve, suspend, or ban merchant", description = "Toggles merchant account active/frozen status with mandatory audit logging")
    public ResponseEntity<ApiResponse<Void>> updateMerchantStatus(
            @PathVariable Long id,
            @Valid @RequestBody MerchantStatusUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long adminId = userDetails.getId();
        String adminEmail = userDetails.getEmail();
        String ipAddress = httpRequest.getRemoteAddr();
        adminGovernanceService.updateMerchantStatus(adminId, adminEmail, id, request.getEnabled(), request.getReason(), ipAddress);

        String message = request.getEnabled() ? "Merchant approved successfully" : "Merchant suspended successfully";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @GetMapping("/disputes")
    @Operation(summary = "Get dispute tickets queue", description = "Retrieves dispute tickets with optional status filtering (e.g. ESCALATED_TO_ADMIN)")
    public ResponseEntity<ApiResponse<Page<DisputeResponseDTO>>> getDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return ResponseEntity.ok(ApiResponse.success("Disputes retrieved successfully", adminGovernanceService.getDisputes(status, pageable)));
    }

    @PostMapping("/disputes/{id}/resolve")
    @Operation(summary = "Administrative Dispute Resolution & Refund Override", description = "Overrides or approves refund for escalated customer-merchant disputes")
    public ResponseEntity<ApiResponse<DisputeResponseDTO>> resolveDispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeAdminResolveRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long adminId = userDetails.getId();
        String adminEmail = userDetails.getEmail();
        String ipAddress = httpRequest.getRemoteAddr();
        DisputeResponseDTO resolved = adminGovernanceService.adminResolveDispute(adminId, adminEmail, id, request, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Dispute resolved successfully", resolved));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get admin audit log history", description = "Retrieves immutable audit records of all administrative actions")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogDTO>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", adminGovernanceService.getAuditLogs(pageable)));
    }
}
