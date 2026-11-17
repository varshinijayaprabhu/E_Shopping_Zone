package com.eshopping.product.service.impl;

import com.eshopping.product.dto.*;
import com.eshopping.product.entity.Category;
import com.eshopping.product.entity.Inventory;
import com.eshopping.product.entity.Product;
import com.eshopping.product.exception.DuplicateResourceException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.exception.UnauthorizedMerchantException;
import com.eshopping.product.repository.CategoryRepository;
import com.eshopping.product.repository.ProductRepository;
import com.eshopping.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductResponseDTO>> getProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> page = productRepository.findAll(spec, pageable);
        List<ProductResponseDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ApiResponse.ok("Products retrieved successfully", dtos, PaginationMeta.fromPage(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductResponseDTO>> searchProducts(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getProducts(null, null, null, pageable);
        }

        Page<Product> page = productRepository.searchActiveByKeyword(query.trim(), pageable);
        List<ProductResponseDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ApiResponse.ok("Search results retrieved", dtos, PaginationMeta.fromPage(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductResponseDTO>> getMerchantProducts(Long merchantId, Pageable pageable) {
        Page<Product> page = productRepository.findByMerchantId(merchantId, pageable);
        List<ProductResponseDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ApiResponse.ok("Merchant products retrieved successfully", dtos, PaginationMeta.fromPage(page));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found or inactive"));
        return mapToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductSnapshot(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
        return mapToDTO(product);
    }

    @Override
    public ProductResponseDTO createProduct(ProductCreateRequestDTO request, Long merchantId) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.getCategoryId() + " not found"));

        Product product = new Product(
                merchantId,
                category,
                request.getName(),
                request.getSku().toUpperCase().trim(),
                request.getDescription(),
                request.getPrice(),
                request.getImageUrl(),
                true
        );

        Inventory inventory = new Inventory(product, request.getInitialStock(), 0);
        product.setInventory(inventory);

        Product saved = productRepository.save(product);
        log.info("Merchant ID {} created new product ID {} with initial stock {}",
                merchantId, saved.getId(), request.getInitialStock());

        return mapToDTO(saved);
    }

    @Override
    public ProductResponseDTO updateProduct(Long id, ProductUpdateRequestDTO request, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));

        // Enforce Merchant Ownership (Anti-IDOR Defense)
        if (!product.getMerchantId().equals(authenticatedMerchantId)) {
            log.error("Access denied: Merchant ID {} attempted to modify product ID {} owned by merchant ID {}",
                    authenticatedMerchantId, id, product.getMerchantId());
            throw new UnauthorizedMerchantException("Merchant ID " + authenticatedMerchantId + " is not authorized to modify product ID " + id);
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.getCategoryId() + " not found"));
            product.setCategory(category);
        }
        if (request.getAvailableStock() != null) {
            if (product.getInventory() != null) {
                product.getInventory().setAvailableStock(request.getAvailableStock());
            } else {
                Inventory inventory = new Inventory(product, request.getAvailableStock(), 0);
                product.setInventory(inventory);
            }
        }

        Product saved = productRepository.save(product);
        log.info("Merchant ID {} updated product ID {}", authenticatedMerchantId, saved.getId());
        return mapToDTO(saved);
    }

    @Override
    public void deleteProduct(Long id, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));

        // Enforce Merchant Ownership
        if (!product.getMerchantId().equals(authenticatedMerchantId)) {
            log.error("Access denied: Merchant ID {} attempted to delete product ID {} owned by merchant ID {}",
                    authenticatedMerchantId, id, product.getMerchantId());
            throw new UnauthorizedMerchantException("Merchant ID " + authenticatedMerchantId + " is not authorized to delete product ID " + id);
        }

        // Soft delete active status to preserve order history
        product.setActive(false);
        productRepository.save(product);
        log.info("Merchant ID {} soft-deleted product ID {}", authenticatedMerchantId, id);
    }

    private ProductResponseDTO mapToDTO(Product p) {
        int available = 0;
        int reserved = 0;
        if (p.getInventory() != null) {
            available = p.getInventory().getAvailableStock();
            reserved = p.getInventory().getReservedStock();
        }

        return new ProductResponseDTO(
                p.getId(),
                p.getMerchantId(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getName(),
                p.getSku(),
                p.getDescription(),
                p.getPrice(),
                p.getImageUrl(),
                available,
                reserved,
                p.isActive()
        );
    }
}

