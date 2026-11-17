package com.eshopping.admin.controller;

import com.eshopping.admin.client.OrderServiceClient;
import com.eshopping.admin.client.ProfileServiceClient;
import com.eshopping.admin.dto.DisputeCreateRequestDTO;
import com.eshopping.admin.dto.DisputeMerchantResponseRequestDTO;
import com.eshopping.admin.entity.DisputeStatus;
import com.eshopping.admin.entity.DisputeTicket;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DisputeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DisputeTicketRepository disputeTicketRepository;

    @MockBean
    private ProfileServiceClient profileServiceClient;

    @MockBean
    private OrderServiceClient orderServiceClient;

    private String customerToken;
    private String merchantToken;

    @BeforeEach
    void setUp() {
        disputeTicketRepository.deleteAll();

        customerToken = "Bearer " + jwtTokenProvider.generateTokenForTest(1L, "customer@eshopping.com", Collections.singleton("ROLE_CUSTOMER"));
        merchantToken = "Bearer " + jwtTokenProvider.generateTokenForTest(10L, "merchant@eshopping.com", Collections.singleton("ROLE_MERCHANT"));
    }

    @Test
    @DisplayName("POST /api/disputes/create opens dispute ticket with 201 Created and PENDING_MERCHANT")
    void testCreateDispute() throws Exception {
        DisputeCreateRequestDTO req = new DisputeCreateRequestDTO(
                201L, 10L, "Wrong color sent", "I ordered black, got white", new BigDecimal("799.00")
        );

        mockMvc.perform(post("/api/disputes/create")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is(201)))
                .andExpect(jsonPath("$.data.status", is("PENDING_MERCHANT")))
                .andExpect(jsonPath("$.data.refundAmount", is(799.00)));
    }

    @Test
    @DisplayName("GET /api/disputes/my returns customer's dispute tickets")
    void testGetMyDisputes_Customer() throws Exception {
        DisputeTicket ticket = new DisputeTicket(
                202L, 1L, 10L, "Damaged in transit", null,
                new BigDecimal("499.00"), LocalDateTime.now().plusMinutes(2)
        );
        disputeTicketRepository.save(ticket);

        mockMvc.perform(get("/api/disputes/my")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].orderId", is(202)));
    }

    @Test
    @DisplayName("POST /api/disputes/{id}/merchant-response allows merchant to ACCEPT dispute")
    void testMerchantRespond_Accept() throws Exception {
        DisputeTicket ticket = new DisputeTicket(
                203L, 1L, 10L, "Wrong item", null,
                new BigDecimal("600.00"), LocalDateTime.now().plusMinutes(2)
        );
        DisputeTicket savedTicket = disputeTicketRepository.save(ticket);

        DisputeMerchantResponseRequestDTO req = new DisputeMerchantResponseRequestDTO("ACCEPT", "Sorry for mistake, refunded");

        mockMvc.perform(post("/api/disputes/" + savedTicket.getId() + "/merchant-response")
                        .header("Authorization", merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("MERCHANT_ACCEPTED")));
    }

    @Test
    @DisplayName("GET /api/disputes/{id} returns dispute details with remaining SLA seconds")
    void testGetDisputeById() throws Exception {
        DisputeTicket ticket = new DisputeTicket(
                204L, 1L, 10L, "Defective product", null,
                new BigDecimal("1200.00"), LocalDateTime.now().plusMinutes(2)
        );
        DisputeTicket savedTicket = disputeTicketRepository.save(ticket);

        mockMvc.perform(get("/api/disputes/" + savedTicket.getId())
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(savedTicket.getId().intValue())))
                .andExpect(jsonPath("$.data.remainingSeconds", greaterThanOrEqualTo(0)));
    }
}
