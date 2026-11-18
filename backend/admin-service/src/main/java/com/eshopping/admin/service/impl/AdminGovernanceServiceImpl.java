package com.eshopping.admin.service.impl;

import com.eshopping.admin.client.OrderServiceClient;
import com.eshopping.admin.client.ProfileServiceClient;
import com.eshopping.admin.dto.*;
import com.eshopping.admin.entity.AdminAuditLog;
import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.entity.DisputeTicket;
import com.eshopping.admin.exception.AccessDeniedException;
import com.eshopping.admin.exception.BadRequestException;
import com.eshopping.admin.exception.ResourceNotFoundException;
import com.eshopping.admin.repository.AdminAuditLogRepository;
import com.eshopping.admin.repository.DisputeTicketRepository;
import com.eshopping.admin.service.AdminGovernanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminGovernanceServiceImpl implements AdminGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(AdminGovernanceServiceImpl.class);

    private final AdminAuditLogRepository auditLogRepository;
    private final DisputeTicketRepository disputeTicketRepository;
    private final ProfileServiceClient profileServiceClient;
    private final OrderServiceClient orderServiceClient;

    @Value("${dispute.sla.minutes:2}")
    private int disputeSlaMinutes;

    public AdminGovernanceServiceImpl(AdminAuditLogRepository auditLogRepository,
                                      DisputeTicketRepository disputeTicketRepository,
                                      ProfileServiceClient profileServiceClient,
                                      OrderServiceClient orderServiceClient) {
        this.auditLogRepository = auditLogRepository;
        this.disputeTicketRepository = disputeTicketRepository;
        this.profileServiceClient = profileServiceClient;
        this.orderServiceClient = orderServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        OrderMetricsSummaryDTO orderMetrics = orderServiceClient.getMetricsSummary();
        UserCountsDTO userCounts = profileServiceClient.getUserCounts();

        long pendingDisputes = disputeTicketRepository.countByStatus(DisputeStatus.PENDING_MERCHANT);
        long escalatedDisputes = disputeTicketRepository.countByStatus(DisputeStatus.ESCALATED_TO_ADMIN);
        long resolvedDisputes = disputeTicketRepository.countByStatus(DisputeStatus.REFUNDED_BY_ADMIN)
                + disputeTicketRepository.countByStatus(DisputeStatus.RESOLVED_NO_REFUND)
                + disputeTicketRepository.countByStatus(DisputeStatus.MERCHANT_ACCEPTED);

        return new DashboardStatsDTO(
                orderMetrics.getTotalOrders(),
                orderMetrics.getTotalRevenue(),
                userCounts.getTotalUsers(),
                userCounts.getActiveCustomers(),
                userCounts.getRegisteredMerchants(),
                userCounts.getPendingMerchants(),
                pendingDisputes,
                escalatedDisputes,
                resolvedDisputes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantProfileResponseDTO> getAllMerchants() {
        return profileServiceClient.getAllMerchants();
    }

    @Override
    @Transactional
    public void updateMerchantStatus(Long adminId, Long merchantId, boolean enabled, String reason, String ipAddress) {
        updateMerchantStatus(adminId, "admin@eshopping.com", merchantId, enabled, reason, ipAddress);
    }

    @Override
    @Transactional
    public void updateMerchantStatus(Long adminId, String adminEmail, Long merchantId, boolean enabled, String reason, String ipAddress) {
        profileServiceClient.updateMerchantStatus(merchantId, enabled);

        String actionType = enabled ? "MERCHANT_APPROVE" : "MERCHANT_SUSPEND";
        String auditReason = (reason != null && !reason.trim().isEmpty())
                ? reason
                : (enabled ? "Merchant account approved by admin" : "Merchant account suspended by admin");

        String effectiveEmail = (adminEmail != null && !adminEmail.trim().isEmpty()) ? adminEmail : "admin@eshopping.com";

        AdminAuditLog auditLog = new AdminAuditLog(
                adminId,
                effectiveEmail,
                actionType,
                "MERCHANT",
                merchantId,
                auditReason,
                "{\"enabled\": " + enabled + "}",
                ipAddress
        );
        auditLogRepository.save(auditLog);
        log.info("Admin {} ({}) updated merchant {} status to enabled={} (Reason: {})", adminId, effectiveEmail, merchantId, enabled, auditReason);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditLogDTO> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToAuditLogDTO);
    }

    @Override
    @Transactional
    public DisputeResponseDTO createDispute(Long customerId, DisputeCreateRequestDTO request) {
        if (disputeTicketRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new BadRequestException("A dispute ticket already exists for order #" + request.getOrderId());
        }

        LocalDateTime deadline = LocalDateTime.now().plusMinutes(disputeSlaMinutes);

        DisputeTicket ticket = new DisputeTicket(
                request.getOrderId(),
                customerId,
                request.getMerchantId(),
                request.getIssueReason(),
                request.getCustomerComment(),
                request.getRefundAmount(),
                deadline
        );

        DisputeTicket saved = disputeTicketRepository.save(ticket);
        log.info("Dispute ticket #{} opened by customer {} for order {} with deadline {}",
                saved.getId(), customerId, request.getOrderId(), deadline);

        return mapToDisputeDTO(saved);
    }

    @Override
    @Transactional
    public DisputeResponseDTO merchantRespond(Long merchantId, Long disputeId, DisputeMerchantResponseRequestDTO request) {
        DisputeTicket ticket = disputeTicketRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute ticket not found with id: " + disputeId));

        if (!ticket.getMerchantId().equals(merchantId)) {
            throw new AccessDeniedException("You are not authorized to respond to this dispute");
        }

        if (ticket.getStatus() != DisputeStatus.PENDING_MERCHANT) {
            throw new BadRequestException("Dispute is not pending merchant response. Current status: " + ticket.getStatus());
        }

        ticket.setMerchantResponse(request.getResponseComment());

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            ticket.setStatus(DisputeStatus.MERCHANT_ACCEPTED);
            ticket.setResolvedAt(LocalDateTime.now());
            // Trigger refund in order-service
            orderServiceClient.adminDisputeRefund(ticket.getOrderId(), "Merchant accepted dispute: " + request.getResponseComment());
            log.info("Merchant {} accepted dispute #{}. Refund triggered for order {}", merchantId, disputeId, ticket.getOrderId());
        } else {
            // Reject -> escalate to admin
            ticket.setStatus(DisputeStatus.ESCALATED_TO_ADMIN);
            ticket.setEscalatedAt(LocalDateTime.now());
            log.info("Merchant {} rejected dispute #{}. Escalated to administrator queue", merchantId, disputeId);
        }

        DisputeTicket saved = disputeTicketRepository.save(ticket);
        return mapToDisputeDTO(saved);
    }

    @Override
    @Transactional
    public DisputeResponseDTO adminResolveDispute(Long adminId, Long disputeId, DisputeAdminResolveRequestDTO request, String ipAddress) {
        return adminResolveDispute(adminId, "admin@eshopping.com", disputeId, request, ipAddress);
    }

    @Override
    @Transactional
    public DisputeResponseDTO adminResolveDispute(Long adminId, String adminEmail, Long disputeId, DisputeAdminResolveRequestDTO request, String ipAddress) {
        DisputeTicket ticket = disputeTicketRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute ticket not found with id: " + disputeId));

        if (ticket.getStatus() != DisputeStatus.ESCALATED_TO_ADMIN && ticket.getStatus() != DisputeStatus.PENDING_MERCHANT) {
            throw new BadRequestException("Dispute cannot be resolved in its current status: " + ticket.getStatus());
        }

        ticket.setAdminNotes(request.getAdminNotes());
        ticket.setResolvedAt(LocalDateTime.now());

        String action = request.getAction().toUpperCase();
        if ("REFUND".equals(action)) {
            ticket.setStatus(DisputeStatus.REFUNDED_BY_ADMIN);
            orderServiceClient.adminDisputeRefund(ticket.getOrderId(), "Admin dispute resolution: " + request.getAdminNotes());
            log.info("Admin {} ({}) approved refund for dispute #{}, order {}", adminId, adminEmail, disputeId, ticket.getOrderId());
        } else {
            ticket.setStatus(DisputeStatus.RESOLVED_NO_REFUND);
            log.info("Admin {} ({}) closed dispute #{} with NO refund", adminId, adminEmail, disputeId);
        }

        DisputeTicket saved = disputeTicketRepository.save(ticket);

        String effectiveEmail = (adminEmail != null && !adminEmail.trim().isEmpty()) ? adminEmail : "admin@eshopping.com";
        AdminAuditLog auditLog = new AdminAuditLog(
                adminId,
                effectiveEmail,
                "DISPUTE_RESOLUTION_OVERRIDE",
                "DISPUTE",
                disputeId,
                "Resolved as " + ticket.getStatus() + ": " + request.getAdminNotes(),
                "{\"orderId\": " + ticket.getOrderId() + ", \"action\": \"" + action + "\"}",
                ipAddress
        );
        auditLogRepository.save(auditLog);

        return mapToDisputeDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponseDTO> getCustomerDisputes(Long customerId) {
        return disputeTicketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToDisputeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponseDTO> getMerchantDisputes(Long merchantId) {
        return disputeTicketRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::mapToDisputeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DisputeResponseDTO> getDisputes(DisputeStatus status, Pageable pageable) {
        if (status != null) {
            return disputeTicketRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::mapToDisputeDTO);
        }
        return disputeTicketRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDisputeDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponseDTO getDisputeById(Long id) {
        DisputeTicket ticket = disputeTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute ticket not found with id: " + id));
        return mapToDisputeDTO(ticket);
    }

    @Override
    @Transactional
    public int escalateExpiredDisputes() {
        LocalDateTime now = LocalDateTime.now();
        List<DisputeTicket> expired = disputeTicketRepository.findByStatusAndMerchantDeadlineBefore(
                DisputeStatus.PENDING_MERCHANT, now);

        for (DisputeTicket ticket : expired) {
            ticket.setStatus(DisputeStatus.ESCALATED_TO_ADMIN);
            ticket.setEscalatedAt(now);
            disputeTicketRepository.save(ticket);
            log.info("Dispute #{} automatically escalated to Admin due to merchant SLA expiry (Deadline was: {})",
                    ticket.getId(), ticket.getMerchantDeadline());
        }

        return expired.size();
    }

    private DisputeResponseDTO mapToDisputeDTO(DisputeTicket t) {
        Long remainingSeconds = 0L;
        if (t.getStatus() == DisputeStatus.PENDING_MERCHANT && t.getMerchantDeadline() != null) {
            long sec = Duration.between(LocalDateTime.now(), t.getMerchantDeadline()).getSeconds();
            remainingSeconds = Math.max(0L, sec);
        }

        return new DisputeResponseDTO(
                t.getId(),
                t.getOrderId(),
                t.getCustomerId(),
                t.getMerchantId(),
                t.getIssueReason(),
                t.getCustomerComment(),
                t.getMerchantResponse(),
                t.getAdminNotes(),
                t.getRefundAmount(),
                t.getStatus(),
                t.getMerchantDeadline(),
                remainingSeconds,
                t.getEscalatedAt(),
                t.getResolvedAt(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private AdminAuditLogDTO mapToAuditLogDTO(AdminAuditLog a) {
        return new AdminAuditLogDTO(
                a.getId(),
                a.getAdminId(),
                a.getAdminEmail(),
                a.getActionType(),
                a.getTargetEntityType(),
                a.getTargetEntityId(),
                a.getReason(),
                a.getMetadata(),
                a.getIpAddress(),
                a.getCreatedAt()
        );
    }
}
