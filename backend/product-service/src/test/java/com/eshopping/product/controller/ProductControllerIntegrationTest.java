package com.eshopping.product.controller;

import com.eshopping.product.dto.*;
import com.eshopping.product.security.JwtTokenProvider;
import com.eshopping.product.service.InventoryService;
import com.eshopping.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ProductService productService;

    @MockBean
    private InventoryService inventoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/products: Public catalog browsing returns 200 OK")
    void testGetProducts_PublicAccess() throws Exception {
        ProductResponseDTO productDTO = new ProductResponseDTO(
                1L, 10L, 1L, "Electronics", "Wireless Headphones", "ELEC-HP-001",
                "Noise cancelling", new BigDecimal("9999.00"), "http://img.com/1.png", 50, 0, true
        );
        ApiResponse<java.util.List<ProductResponseDTO>> apiResponse = ApiResponse.ok(
                "Products retrieved successfully",
                Collections.singletonList(productDTO),
                new PaginationMeta(0, 10, 1L, 1, true, true)
        );

        when(productService.getProducts(any(), any(), any(), any())).thenReturn(apiResponse);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Wireless Headphones"));
    }

    @Test
    @DisplayName("GET /api/products/{id}: Public access returns 200 OK with product details")
    void testGetProductById_PublicAccess() throws Exception {
        ProductResponseDTO productDTO = new ProductResponseDTO(
                1L, 10L, 1L, "Electronics", "Wireless Headphones", "ELEC-HP-001",
                "Noise cancelling", new BigDecimal("9999.00"), "http://img.com/1.png", 50, 0, true
        );

        when(productService.getProductById(1L)).thenReturn(productDTO);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sku").value("ELEC-HP-001"));
    }

    @Test
    @DisplayName("POST /api/products: Unauthenticated request returns 401 Unauthorized")
    void testCreateProduct_Unauthenticated_Returns401() throws Exception {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                1L, "New Product", "NEW-SKU-001", "Desc", new BigDecimal("199.00"), null, 10
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/products: Customer role attempting product creation returns 403 Forbidden")
    void testCreateProduct_CustomerRole_Returns403() throws Exception {
        String customerJwt = jwtTokenProvider.generateTokenForTest(1L, "customer@example.com", Collections.singleton("ROLE_CUSTOMER"));

        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                1L, "New Product", "NEW-SKU-001", "Desc", new BigDecimal("199.00"), null, 10
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products: Merchant role successfully creates product and returns 201 Created")
    void testCreateProduct_MerchantRole_Returns201() throws Exception {
        String merchantJwt = jwtTokenProvider.generateTokenForTest(10L, "merchant@example.com", Collections.singleton("ROLE_MERCHANT"));

        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                1L, "New Product", "NEW-SKU-001", "Desc", new BigDecimal("199.00"), null, 10
        );

        ProductResponseDTO created = new ProductResponseDTO(
                5L, 10L, 1L, "Electronics", "New Product", "NEW-SKU-001",
                "Desc", new BigDecimal("199.00"), null, 10, 0, true
        );

        when(productService.createProduct(any(), eq(10L))).thenReturn(created);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + merchantJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.merchantId").value(10));
    }

    @Test
    @DisplayName("POST /api/products/reserve: External call without internal secret returns 403 Forbidden")
    void testReserveInventory_ExternalAccess_Returns403() throws Exception {
        InventoryReserveRequestDTO request = new InventoryReserveRequestDTO(
                "ORD-999",
                Collections.singletonList(new InventoryReserveRequestDTO.ItemReservation(1L, 2))
        );

        mockMvc.perform(post("/api/products/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products/reserve: Internal Saga call with X-Internal-Secret returns 200 OK")
    void testReserveInventory_InternalSecret_Returns200() throws Exception {
        InventoryReserveRequestDTO request = new InventoryReserveRequestDTO(
                "ORD-999",
                Collections.singletonList(new InventoryReserveRequestDTO.ItemReservation(1L, 2))
        );

        InventoryReservationResponseDTO response = new InventoryReservationResponseDTO(
                "ORD-999",
                Collections.singletonList(new InventoryReservationResponseDTO.ReservedItemResult(1L, 2, 48))
        );

        when(inventoryService.reserveInventory(any())).thenReturn(response);

        mockMvc.perform(post("/api/products/reserve")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-999"))
                .andExpect(jsonPath("$.data.reservedItems[0].remainingStock").value(48));
    }

    @Test
    @DisplayName("POST /api/products/release: Internal Saga call with X-Internal-Service returns 200 OK")
    void testReleaseInventory_InternalService_Returns200() throws Exception {
        InventoryReleaseRequestDTO request = new InventoryReleaseRequestDTO(
                "ORD-999",
                Collections.singletonList(new InventoryReleaseRequestDTO.ItemRelease(1L, 2))
        );

        InventoryReleaseResponseDTO response = new InventoryReleaseResponseDTO(
                "ORD-999",
                Collections.singletonList(new InventoryReleaseResponseDTO.ReleasedItemResult(1L, 2, 50))
        );

        when(inventoryService.releaseInventory(any())).thenReturn(response);

        mockMvc.perform(post("/api/products/release")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .header("X-Internal-Service", "order-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-999"))
                .andExpect(jsonPath("$.data.releasedItems[0].availableStock").value(50));
    }
}

