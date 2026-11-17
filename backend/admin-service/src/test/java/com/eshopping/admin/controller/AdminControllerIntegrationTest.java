package com.eshopping.admin.controller;

import com.eshopping.admin.client.OrderServiceClient;
import com.eshopping.admin.client.ProfileServiceClient;
import com.eshopping.admin.dto.*;
import com.eshopping.admin.entity.AdminAuditLog;
import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.entity.DisputeTicket;
import com.eshopping.admin.repository.AdminAuditLogRepository;
import com.eshopping.admin.repository.DisputeTicketRepository;
import com.eshopping.admin.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    @Autowired
    private DisputeTicketRepository disputeTicketRepository;

    @MockBean
    private ProfileServiceClient profileServiceClient;

    @MockBean
    private OrderServiceClient orderServiceClient;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        disputeTicketRepository.deleteAll();

        adminToken = "Bearer " + jwtTokenProvider.generateTokenForTest(4L, "admin@eshopping.com", Collections.singleton("ROLE_ADMIN"));
        customerToken = "Bearer " + jwtTokenProvider.generateTokenForTest(1L, "customer@eshopping.com", Collections.singleton("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("GET /api/admin/metrics returns 200 with platform KPI big numbers for ROLE_ADMIN")
    void testGetDashboardMetrics_Success() throws Exception {
        when(orderServiceClient.getMetricsSummary())
                .thenReturn(new OrderMetricsSummaryDTO(50, new BigDecimal("125000.00")));
        when(profileServiceClient.getUserCounts())
                .thenReturn(new UserCountsDTO(200, 190, 10, 1));

        mockMvc.perform(get("/api/admin/metrics")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalOrders", is(50)))
                .andExpect(jsonPath("$.data.totalRevenue", is(125000.00)))
                .andExpect(jsonPath("$.data.registeredMerchants", is(10)));
    }

    @Test
    @DisplayName("GET /api/admin/metrics returns 403 Forbidden for non-admin user")
    void testGetDashboardMetrics_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/merchants lists all merchants")
    void testGetAllMerchants() throws Exception {
        MerchantProfileResponseDTO m1 = new MerchantProfileResponseDTO(
                10L, "merchant@eshopping.com", "Acme Store", "9876543210",
                "Bengaluru", "Karnataka", "India", Collections.singleton("ROLE_MERCHANT"),
                true, LocalDateTime.now()
        );
        when(profileServiceClient.getAllMerchants()).thenReturn(Collections.singletonList(m1));

        mockMvc.perform(get("/api/admin/merchants")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("merchant@eshopping.com")));
    }

    @Test
    @DisplayName("PUT /api/admin/merchants/{id}/status toggles merchant enabled flag and logs audit")
    void testUpdateMerchantStatus() throws Exception {
        MerchantStatusUpdateRequestDTO req = new MerchantStatusUpdateRequestDTO(false, "Suspicious transactions");

        mockMvc.perform(put("/api/admin/merchants/10/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Merchant suspended successfully")));

        // Verify audit log in repository
        org.junit.jupiter.api.Assertions.assertEquals(1, auditLogRepository.count());
        AdminAuditLog log = auditLogRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("MERCHANT_SUSPEND", log.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals(10L, log.getTargetEntityId());
    }

    @Test
    @DisplayName("POST /api/admin/disputes/{id}/resolve resolves dispute with REFUND")
    void testResolveDispute_Refund() throws Exception {
        DisputeTicket ticket = new DisputeTicket(
                101L, 1L, 10L, "Damaged item", "Torn box",
                new BigDecimal("999.00"), LocalDateTime.now().minusMinutes(5)
        );
        ticket.setStatus(DisputeStatus.ESCALATED_TO_ADMIN);
        DisputeTicket savedTicket = disputeTicketRepository.save(ticket);

        DisputeAdminResolveRequestDTO req = new DisputeAdminResolveRequestDTO("REFUND", "Defect approved by admin");

        mockMvc.perform(post("/api/admin/disputes/" + savedTicket.getId() + "/resolve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REFUNDED_BY_ADMIN")));
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs returns audit log history")
    void testGetAuditLogs() throws Exception {
        auditLogRepository.save(new AdminAuditLog(
                4L, "MERCHANT_APPROVE", "MERCHANT", 10L, "Approved documents", "{}", "127.0.0.1"
        ));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].actionType", is("MERCHANT_APPROVE")));
    }
}
