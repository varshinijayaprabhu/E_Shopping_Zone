package com.eshopping.cart.controller;

import com.eshopping.cart.dto.AddToCartRequestDTO;
import com.eshopping.cart.dto.CartItemDTO;
import com.eshopping.cart.dto.CartResponseDTO;
import com.eshopping.cart.dto.UpdateCartItemRequestDTO;
import com.eshopping.cart.security.JwtTokenProvider;
import com.eshopping.cart.service.CartService;
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
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CartService cartService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/cart: Unauthenticated access returns 401 Unauthorized")
    void testGetCart_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/cart: Authenticated customer retrieves active cart (200 OK)")
    void testGetCart_Authenticated_Returns200() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));

        CartItemDTO itemDTO = new CartItemDTO(
                1L, 10L, "Headphones", "ELEC-HP-001", new BigDecimal("9999.00"),
                "http://img.com/1.png", 2, new BigDecimal("19998.00"), true, 50
        );
        CartResponseDTO cartResponse = new CartResponseDTO(
                1L, 100L, Collections.singletonList(itemDTO), 2, new BigDecimal("19998.00")
        );

        when(cartService.getCart(100L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").value(100))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.cartSubtotal").value(19998.00));
    }

    @Test
    @DisplayName("POST /api/cart/items: Valid item addition returns 200 OK with updated cart")
    void testAddItem_Valid_Returns200() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));
        AddToCartRequestDTO request = new AddToCartRequestDTO(10L, 1);

        CartResponseDTO cartResponse = new CartResponseDTO(
                1L, 100L, Collections.emptyList(), 1, new BigDecimal("9999.00")
        );

        when(cartService.addItemToCart(eq(100L), any())).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to cart successfully"));
    }

    @Test
    @DisplayName("POST /api/cart/items: Negative or zero quantity returns 400 Bad Request (Validation failure)")
    void testAddItem_InvalidQuantity_Returns400() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));
        AddToCartRequestDTO request = new AddToCartRequestDTO(10L, 0); // Invalid: quantity must be at least 1

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
    }

    @Test
    @DisplayName("PUT /api/cart/items/{itemId}: Update quantity returns 200 OK")
    void testUpdateQuantity_Valid_Returns200() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));
        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO(3);

        CartResponseDTO cartResponse = new CartResponseDTO(
                1L, 100L, Collections.emptyList(), 3, new BigDecimal("29997.00")
        );

        when(cartService.updateItemQuantity(eq(100L), eq(1L), any())).thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/items/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId}: Remove item returns 200 OK")
    void testRemoveItem_Valid_Returns200() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));

        CartResponseDTO cartResponse = new CartResponseDTO(
                1L, 100L, Collections.emptyList(), 0, BigDecimal.ZERO
        );

        when(cartService.removeItem(100L, 1L)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/items/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed from cart successfully"));
    }

    @Test
    @DisplayName("DELETE /api/cart: Clear cart returns 204 No Content")
    void testClearCart_Returns204() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(100L, "alice@example.com", Collections.singleton("ROLE_CUSTOMER"));

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}

