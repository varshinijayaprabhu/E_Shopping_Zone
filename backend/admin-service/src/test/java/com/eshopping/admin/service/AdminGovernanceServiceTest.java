package com.eshopping.admin.service;

import com.eshopping.admin.client.OrderServiceClient;
import com.eshopping.admin.client.ProfileServiceClient;
import com.eshopping.admin.dto.*;
import com.eshopping.admin.entity.AdminAuditLog;
import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.entity.DisputeTicket;
import com.eshopping.admin.exception.BadRequestException;
import com.eshopping.admin.exception.ResourceNotFoundException;
import com.eshopping.admin.repository.AdminAuditLogRepository;
import com.eshopping.admin.repository.DisputeTicketRepository;
import com.eshopping.admin.service.impl.AdminGovernanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGovernanceServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private DisputeTicketRepository disputeTicketRepository;

    @Mock
    private ProfileServiceClient profileServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private AdminGovernanceServiceImpl governanceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(governanceService, "disputeSlaMinutes", 2);
    }

    @Test
    @DisplayName("getDashboardStats aggregates metrics from Order, Profile services, and Dispute repo")
    void testGetDashboardStats() {
        when(orderServiceClient.getMetricsSummary())
                .thenReturn(new OrderMetricsSummaryDTO(150, new BigDecimal("450000.00")));
        when(profileServiceClient.getUserCounts())
                .thenReturn(new UserCountsDTO(500, 480, 20, 2));

        when(disputeTicketRepository.countByStatus(DisputeStatus.PENDING_MERCHANT)).thenReturn(3L);
        when(disputeTicketRepository.countByStatus(DisputeStatus.ESCALATED_TO_ADMIN)).thenReturn(1L);
        when(disputeTicketRepository.countByStatus(DisputeStatus.REFUNDED_BY_ADMIN)).thenReturn(5L);
        when(disputeTicketRepository.countByStatus(DisputeStatus.RESOLVED_NO_REFUND)).thenReturn(2L);
        when(disputeTicketRepository.countByStatus(DisputeStatus.MERCHANT_ACCEPTED)).thenReturn(4L);

        DashboardStatsDTO stats = governanceService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(150, stats.getTotalOrders());
        assertEquals(new BigDecimal("450000.00"), stats.getTotalRevenue());
        assertEquals(500, stats.getTotalUsers());
        assertEquals(480, stats.getActiveCustomers());
        assertEquals(20, stats.getRegisteredMerchants());
        assertEquals(2, stats.getPendingMerchants());
        assertEquals(3, stats.getPendingDisputes());
        assertEquals(1, stats.getEscalatedDisputes());
        assertEquals(11, stats.getResolvedDisputes()); // 5 + 2 + 4
    }

    @Test
    @DisplayName("updateMerchantStatus approves merchant and persists audit log")
    void testUpdateMerchantStatus_Approve() {
        governanceService.updateMerchantStatus(4L, 10L, true, "Valid documents verified", "127.0.0.1");

        verify(profileServiceClient, times(1)).updateMerchantStatus(10L, true);

        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AdminAuditLog savedLog = logCaptor.getValue();
        assertEquals(4L, savedLog.getAdminId());
        assertEquals("MERCHANT_APPROVE", savedLog.getActionType());
        assertEquals("MERCHANT", savedLog.getTargetEntityType());
        assertEquals(10L, savedLog.getTargetEntityId());
        assertEquals("Valid documents verified", savedLog.getReason());
        assertEquals("127.0.0.1", savedLog.getIpAddress());
    }

    @Test
    @DisplayName("updateMerchantStatus suspends merchant and persists audit log")
    void testUpdateMerchantStatus_Suspend() {
        governanceService.updateMerchantStatus(4L, 10L, false, "Fraudulent activity detected", "192.168.1.5");

        verify(profileServiceClient, times(1)).updateMerchantStatus(10L, false);

        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AdminAuditLog savedLog = logCaptor.getValue();
        assertEquals("MERCHANT_SUSPEND", savedLog.getActionType());
        assertEquals(10L, savedLog.getTargetEntityId());
        assertEquals("Fraudulent activity detected", savedLog.getReason());
    }

    @Test
    @DisplayName("createDispute opens ticket with PENDING_MERCHANT and 2-min deadline")
    void testCreateDispute_Success() {
        DisputeCreateRequestDTO req = new DisputeCreateRequestDTO(
                101L, 20L, "Damaged item received", "Package arrived torn", new BigDecimal("1200.00")
        );

        when(disputeTicketRepository.findByOrderId(101L)).thenReturn(Optional.empty());

        DisputeTicket mockSaved = new DisputeTicket(
                101L, 1L, 20L, "Damaged item received", "Package arrived torn",
                new BigDecimal("1200.00"), LocalDateTime.now().plusMinutes(2)
        );
        mockSaved.setId(1L);

        when(disputeTicketRepository.save(any(DisputeTicket.class))).thenReturn(mockSaved);

        DisputeResponseDTO result = governanceService.createDispute(1L, req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(101L, result.getOrderId());
        assertEquals(DisputeStatus.PENDING_MERCHANT, result.getStatus());
        assertEquals(new BigDecimal("1200.00"), result.getRefundAmount());
    }

    @Test
    @DisplayName("createDispute rejects duplicate dispute for same order")
    void testCreateDispute_DuplicateThrows() {
        DisputeCreateRequestDTO req = new DisputeCreateRequestDTO(
                101L, 20L, "Item not received", null, new BigDecimal("500.00")
        );

        when(disputeTicketRepository.findByOrderId(101L)).thenReturn(Optional.of(new DisputeTicket()));

        assertThrows(BadRequestException.class, () -> governanceService.createDispute(1L, req));
        verify(disputeTicketRepository, never()).save(any(DisputeTicket.class));
    }

    @Test
    @DisplayName("merchantRespond ACCEPT triggers wallet refund and updates status to MERCHANT_ACCEPTED")
    void testMerchantRespond_Accept() {
        DisputeTicket ticket = new DisputeTicket(
                101L, 1L, 20L, "Item damaged", "Broken seal",
                new BigDecimal("800.00"), LocalDateTime.now().plusMinutes(2)
        );
        ticket.setId(5L);

        when(disputeTicketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(disputeTicketRepository.save(any(DisputeTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        DisputeMerchantResponseRequestDTO req = new DisputeMerchantResponseRequestDTO("ACCEPT", "Apologies, refunding immediately");
        DisputeResponseDTO response = governanceService.merchantRespond(20L, 5L, req);

        assertNotNull(response);
        assertEquals(DisputeStatus.MERCHANT_ACCEPTED, response.getStatus());
        verify(orderServiceClient, times(1)).adminDisputeRefund(eq(101L), contains("Merchant accepted dispute"));
    }

    @Test
    @DisplayName("merchantRespond REJECT escalates dispute to ESCALATED_TO_ADMIN")
    void testMerchantRespond_Reject() {
        DisputeTicket ticket = new DisputeTicket(
                101L, 1L, 20L, "Item missing", "Did not get item",
                new BigDecimal("800.00"), LocalDateTime.now().plusMinutes(2)
        );
        ticket.setId(6L);

        when(disputeTicketRepository.findById(6L)).thenReturn(Optional.of(ticket));
        when(disputeTicketRepository.save(any(DisputeTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        DisputeMerchantResponseRequestDTO req = new DisputeMerchantResponseRequestDTO("REJECT", "Delivered with signature");
        DisputeResponseDTO response = governanceService.merchantRespond(20L, 6L, req);

        assertNotNull(response);
        assertEquals(DisputeStatus.ESCALATED_TO_ADMIN, response.getStatus());
        verify(orderServiceClient, never()).adminDisputeRefund(anyLong(), anyString());
    }

    @Test
    @DisplayName("adminResolveDispute REFUND triggers order refund, logs audit, and marks REFUNDED_BY_ADMIN")
    void testAdminResolveDispute_Refund() {
        DisputeTicket ticket = new DisputeTicket(
                101L, 1L, 20L, "Defective product", null,
                new BigDecimal("1500.00"), LocalDateTime.now().minusMinutes(5)
        );
        ticket.setId(7L);
        ticket.setStatus(DisputeStatus.ESCALATED_TO_ADMIN);

        when(disputeTicketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(disputeTicketRepository.save(any(DisputeTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        DisputeAdminResolveRequestDTO req = new DisputeAdminResolveRequestDTO("REFUND", "Admin approved full refund based on buyer photo evidence");
        DisputeResponseDTO response = governanceService.adminResolveDispute(4L, 7L, req, "10.0.0.1");

        assertNotNull(response);
        assertEquals(DisputeStatus.REFUNDED_BY_ADMIN, response.getStatus());
        verify(orderServiceClient, times(1)).adminDisputeRefund(eq(101L), contains("Admin approved full refund"));

        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());
        assertEquals("DISPUTE_RESOLUTION_OVERRIDE", logCaptor.getValue().getActionType());
    }

    @Test
    @DisplayName("adminResolveDispute NO_REFUND marks RESOLVED_NO_REFUND without triggering refund")
    void testAdminResolveDispute_NoRefund() {
        DisputeTicket ticket = new DisputeTicket(
                101L, 1L, 20L, "Buyer remorse", null,
                new BigDecimal("1500.00"), LocalDateTime.now().minusMinutes(5)
        );
        ticket.setId(8L);
        ticket.setStatus(DisputeStatus.ESCALATED_TO_ADMIN);

        when(disputeTicketRepository.findById(8L)).thenReturn(Optional.of(ticket));
        when(disputeTicketRepository.save(any(DisputeTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        DisputeAdminResolveRequestDTO req = new DisputeAdminResolveRequestDTO("NO_REFUND", "Dispute rejected, return policy expired");
        DisputeResponseDTO response = governanceService.adminResolveDispute(4L, 8L, req, "10.0.0.1");

        assertNotNull(response);
        assertEquals(DisputeStatus.RESOLVED_NO_REFUND, response.getStatus());
        verify(orderServiceClient, never()).adminDisputeRefund(anyLong(), anyString());
    }

    @Test
    @DisplayName("escalateExpiredDisputes auto-escalates tickets past deadline")
    void testEscalateExpiredDisputes() {
        DisputeTicket expired1 = new DisputeTicket(101L, 1L, 20L, "Damaged", null, new BigDecimal("500.00"), LocalDateTime.now().minusMinutes(3));
        expired1.setId(1L);
        DisputeTicket expired2 = new DisputeTicket(102L, 2L, 20L, "Wrong item", null, new BigDecimal("300.00"), LocalDateTime.now().minusMinutes(1));
        expired2.setId(2L);

        when(disputeTicketRepository.findByStatusAndMerchantDeadlineBefore(eq(DisputeStatus.PENDING_MERCHANT), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expired1, expired2));

        int escalatedCount = governanceService.escalateExpiredDisputes();

        assertEquals(2, escalatedCount);
        assertEquals(DisputeStatus.ESCALATED_TO_ADMIN, expired1.getStatus());
        assertEquals(DisputeStatus.ESCALATED_TO_ADMIN, expired2.getStatus());
        verify(disputeTicketRepository, times(2)).save(any(DisputeTicket.class));
    }
}
