package com.eshopping.admin.service;

import com.eshopping.admin.dto.*;
import com.eshopping.admin.entity.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminGovernanceService {

    DashboardStatsDTO getDashboardStats();

    List<MerchantProfileResponseDTO> getAllMerchants();

    void updateMerchantStatus(Long adminId, Long merchantId, boolean enabled, String reason, String ipAddress);

    void updateMerchantStatus(Long adminId, String adminEmail, Long merchantId, boolean enabled, String reason, String ipAddress);

    Page<AdminAuditLogDTO> getAuditLogs(Pageable pageable);

    DisputeResponseDTO createDispute(Long customerId, DisputeCreateRequestDTO request);

    DisputeResponseDTO merchantRespond(Long merchantId, Long disputeId, DisputeMerchantResponseRequestDTO request);

    DisputeResponseDTO adminResolveDispute(Long adminId, Long disputeId, DisputeAdminResolveRequestDTO request, String ipAddress);

    DisputeResponseDTO adminResolveDispute(Long adminId, String adminEmail, Long disputeId, DisputeAdminResolveRequestDTO request, String ipAddress);

    List<DisputeResponseDTO> getCustomerDisputes(Long customerId);

    List<DisputeResponseDTO> getMerchantDisputes(Long merchantId);

    Page<DisputeResponseDTO> getDisputes(DisputeStatus status, Pageable pageable);

    DisputeResponseDTO getDisputeById(Long id);

    int escalateExpiredDisputes();
}
