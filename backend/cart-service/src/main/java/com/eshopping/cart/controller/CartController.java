package com.eshopping.cart.controller;

import com.eshopping.cart.dto.*;
import com.eshopping.cart.security.CustomUserDetails;
import com.eshopping.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Shopping Cart", description = "Customer shopping cart operations and item management")
@SecurityRequirement(name = "BearerAuth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Get Customer Cart", description = "Retrieves the active cart for the authenticated customer with live prices and stock availability")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        CartResponseDTO cart = cartService.getCart(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Add Item to Cart", description = "Adds a product to the cart or aggregates quantity if already present. Validates stock in real-time.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item added to cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock or invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found or inactive")
    })
    public ResponseEntity<ApiResponse<CartResponseDTO>> addItem(
            @Valid @RequestBody AddToCartRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        CartResponseDTO cart = cartService.addItemToCart(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart successfully", cart));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Update Item Quantity", description = "Updates the quantity of a specific item in the customer's cart. Validates item ownership and stock.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not item owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        CartResponseDTO cart = cartService.updateItemQuantity(customerId, itemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart item quantity updated successfully", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Remove Item from Cart", description = "Removes an item from the customer's cart. Validates item ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not item owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long customerId = userDetails.getId();
        CartResponseDTO cart = cartService.removeItem(customerId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart successfully", cart));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER') or isAuthenticated()")
    @Operation(summary = "Clear Shopping Cart", description = "Empties all items from the authenticated customer's shopping cart")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}

