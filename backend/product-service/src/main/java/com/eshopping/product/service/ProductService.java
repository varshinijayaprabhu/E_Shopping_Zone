package com.eshopping.product.service;

import com.eshopping.product.dto.ApiResponse;
import com.eshopping.product.dto.ProductCreateRequestDTO;
import com.eshopping.product.dto.ProductResponseDTO;
import com.eshopping.product.dto.ProductUpdateRequestDTO;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ApiResponse<List<ProductResponseDTO>> getProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    ApiResponse<List<ProductResponseDTO>> searchProducts(String query, Pageable pageable);

    ApiResponse<List<ProductResponseDTO>> getMerchantProducts(Long merchantId, Pageable pageable);

    ProductResponseDTO getProductById(Long id);

    ProductResponseDTO getProductSnapshot(Long id);

    ProductResponseDTO createProduct(ProductCreateRequestDTO request, Long merchantId);

    ProductResponseDTO updateProduct(Long id, ProductUpdateRequestDTO request, Long authenticatedMerchantId);

    void deleteProduct(Long id, Long authenticatedMerchantId);
}

