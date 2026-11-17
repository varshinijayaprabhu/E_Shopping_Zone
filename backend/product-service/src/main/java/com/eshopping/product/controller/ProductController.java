package com.eshopping.product.controller;

import com.eshopping.product.dto.*;
import com.eshopping.product.security.CustomUserDetails;
import com.eshopping.product.service.InventoryService;
import com.eshopping.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog, merchant management, and stock reservation")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    @org.springframework.beans.factory.annotation.Value("${eshopping.security.internal-key:ESHOPPING_INTERNAL_SECRET_2026}")
    private String internalSecretKey;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List Products (Paginated & Filtered)", description = "Public browsing with pagination, sorting, and price/category filters")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        Sort sort = "DESC".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(productService.getProducts(categoryId, minPrice, maxPrice, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Products by Keyword", description = "Public keyword search matching name or description")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(productService.searchProducts(query, pageable));
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get Merchant Products", description = "Retrieves all products belonging to the authenticated merchant")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getMerchantProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long merchantId = userDetails != null ? userDetails.getId() : 1L;
        Sort sort = "DESC".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.getMerchantProducts(merchantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Product Details", description = "Public retrieval of product details including inventory stock level")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found or inactive")
    })
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @GetMapping("/{id}/snapshot")
    @Operation(summary = "Get Product Snapshot", description = "Internal snapshot retrieval for historical order processing (reads active or inactive)")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductSnapshot(
            @PathVariable Long id,
            javax.servlet.http.HttpServletRequest httpRequest) {
        validateInternalAccess(httpRequest);
        ProductResponseDTO product = productService.getProductSnapshot(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping
    @PreAuthorize("hasRole('MERCHANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create Product", description = "Creates a new product in catalog with initial inventory (Merchant only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role privileges"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductCreateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long merchantId = userDetails != null ? userDetails.getId() : 1L;
        ProductResponseDTO created = productService.createProduct(request, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update Product", description = "Updates product details and available stock. Validates merchant ownership (Merchant only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long merchantId = userDetails != null ? userDetails.getId() : 1L;
        ProductResponseDTO updated = productService.updateProduct(id, request, merchantId);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete Product", description = "Soft-deletes a product from catalog. Validates merchant ownership (Merchant only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not product owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long merchantId = userDetails != null ? userDetails.getId() : 1L;
        productService.deleteProduct(id, merchantId);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully", null));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve Product Inventory", description = "Internal Saga endpoint: Atomically transitions available stock to reserved stock for checkout")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory reserved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock for one or more items"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not internal service"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<InventoryReservationResponseDTO>> reserveInventory(
            @Valid @RequestBody InventoryReserveRequestDTO request,
            javax.servlet.http.HttpServletRequest httpRequest) {

        validateInternalAccess(httpRequest);
        InventoryReservationResponseDTO result = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(ApiResponse.ok("Inventory successfully reserved", result));
    }

    @PostMapping("/release")
    @Operation(summary = "Release Product Inventory", description = "Internal Saga compensating rollback: Reverts reserved stock back to available stock")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not internal service"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<InventoryReleaseResponseDTO>> releaseInventory(
            @Valid @RequestBody InventoryReleaseRequestDTO request,
            javax.servlet.http.HttpServletRequest httpRequest) {

        validateInternalAccess(httpRequest);
        InventoryReleaseResponseDTO result = inventoryService.releaseInventory(request);
        return ResponseEntity.ok(ApiResponse.ok("Reserved inventory released back to available stock", result));
    }

    private void validateInternalAccess(javax.servlet.http.HttpServletRequest httpRequest) {
        String secretHeader = httpRequest.getHeader("X-Internal-Secret");
        if (secretHeader == null || !secretHeader.equals(internalSecretKey)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: Missing or invalid internal service secret");
        }
    }
}

