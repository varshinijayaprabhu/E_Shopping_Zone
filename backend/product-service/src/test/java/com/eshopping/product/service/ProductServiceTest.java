package com.eshopping.product.service;

import com.eshopping.product.dto.ApiResponse;
import com.eshopping.product.dto.ProductCreateRequestDTO;
import com.eshopping.product.dto.ProductResponseDTO;
import com.eshopping.product.dto.ProductUpdateRequestDTO;
import com.eshopping.product.entity.Category;
import com.eshopping.product.entity.Inventory;
import com.eshopping.product.entity.Product;
import com.eshopping.product.exception.DuplicateResourceException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.exception.UnauthorizedMerchantException;
import com.eshopping.product.repository.CategoryRepository;
import com.eshopping.product.repository.ProductRepository;
import com.eshopping.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testCategory = new Category("Electronics", "electronics", "Gadgets", "http://image.com/elec.png");
        testCategory.setId(1L);

        testProduct = new Product(10L, testCategory, "Wireless Headphones", "ELEC-HP-001", "Noise cancelling",
                new BigDecimal("9999.00"), "http://image.com/hp.png", true);
        testProduct.setId(100L);

        testInventory = new Inventory(testProduct, 50, 0);
        testProduct.setInventory(testInventory);
    }

    @Test
    @DisplayName("Get product by ID when active and found returns ProductResponseDTO")
    void testGetProductById_Found() {
        when(productRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(testProduct));

        ProductResponseDTO dto = productService.getProductById(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("Wireless Headphones", dto.getName());
        assertEquals(50, dto.getAvailableStock());
    }

    @Test
    @DisplayName("Get product by ID when not found throws ResourceNotFoundException")
    void testGetProductById_NotFound() {
        when(productRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    @DisplayName("Create product: Successfully creates product and associates initial inventory")
    void testCreateProduct_Success() {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                1L, "Mechanical Keyboard", "ELEC-KB-001", "RGB keyboard",
                new BigDecimal("4999.00"), "http://image.com/kb.png", 25
        );

        when(productRepository.existsBySku("ELEC-KB-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        ProductResponseDTO created = productService.createProduct(request, 10L);

        assertNotNull(created);
        assertEquals(101L, created.getId());
        assertEquals(10L, created.getMerchantId());
        assertEquals("Mechanical Keyboard", created.getName());
        assertEquals(25, created.getAvailableStock());
    }

    @Test
    @DisplayName("Create product with duplicate SKU throws DuplicateResourceException")
    void testCreateProduct_DuplicateSku() {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                1L, "Mechanical Keyboard", "ELEC-HP-001", "RGB keyboard",
                new BigDecimal("4999.00"), "http://image.com/kb.png", 25
        );

        when(productRepository.existsBySku("ELEC-HP-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request, 10L));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update product: Owner merchant successfully updates product details and stock")
    void testUpdateProduct_Success() {
        ProductUpdateRequestDTO request = new ProductUpdateRequestDTO(
                "Updated Headphones", null, "Updated description", new BigDecimal("8999.00"),
                null, 75, true
        );

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO updated = productService.updateProduct(100L, request, 10L);

        assertNotNull(updated);
        assertEquals("Updated Headphones", updated.getName());
        assertEquals(new BigDecimal("8999.00"), updated.getPrice());
        assertEquals(75, updated.getAvailableStock());
    }

    @Test
    @DisplayName("Update product: Different merchant modifying product throws UnauthorizedMerchantException (Anti-IDOR)")
    void testUpdateProduct_UnauthorizedMerchant() {
        ProductUpdateRequestDTO request = new ProductUpdateRequestDTO("Hacked Name", null, null, null, null, null, null);

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        // Authenticated merchant is 99L, but product owner is 10L!
        assertThrows(UnauthorizedMerchantException.class, () ->
                productService.updateProduct(100L, request, 99L)
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete product: Owner merchant successfully soft-deletes product")
    void testDeleteProduct_Success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(100L, 10L);

        assertFalse(testProduct.isActive(), "Product active flag should be set to false");
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Delete product: Non-owner merchant deleting product throws UnauthorizedMerchantException")
    void testDeleteProduct_UnauthorizedMerchant() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        assertThrows(UnauthorizedMerchantException.class, () ->
                productService.deleteProduct(100L, 99L)
        );
        assertTrue(testProduct.isActive());
    }

    @Test
    @DisplayName("Get products with filters returns paged product list")
    @SuppressWarnings("unchecked")
    void testGetProducts_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct), pageable, 1);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        ApiResponse<List<ProductResponseDTO>> response = productService.getProducts(1L, new BigDecimal("1000.00"), new BigDecimal("15000.00"), pageable);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("Wireless Headphones", response.getData().get(0).getName());
    }

    @Test
    @DisplayName("Search products by keyword returns matching results")
    void testSearchProducts_WithKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct), pageable, 1);
        when(productRepository.searchActiveByKeyword(eq("Headphones"), eq(pageable))).thenReturn(page);

        ApiResponse<List<ProductResponseDTO>> response = productService.searchProducts("Headphones", pageable);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("Search products with empty keyword delegates to getProducts")
    @SuppressWarnings("unchecked")
    void testSearchProducts_EmptyKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct), pageable, 1);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        ApiResponse<List<ProductResponseDTO>> response = productService.searchProducts("", pageable);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("Get merchant products returns paged list")
    void testGetMerchantProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct), pageable, 1);
        when(productRepository.findByMerchantId(eq(10L), eq(pageable))).thenReturn(page);

        ApiResponse<List<ProductResponseDTO>> response = productService.getMerchantProducts(10L, pageable);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("Get product snapshot returns DTO for existing product")
    void testGetProductSnapshot_Found() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        ProductResponseDTO dto = productService.getProductSnapshot(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
    }

    @Test
    @DisplayName("Get product snapshot throws ResourceNotFoundException when product not found")
    void testGetProductSnapshot_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductSnapshot(999L));
    }

    @Test
    @DisplayName("Create product with invalid category throws ResourceNotFoundException")
    void testCreateProduct_CategoryNotFound() {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO(
                999L, "Mouse", "ELEC-MS-001", "USB mouse",
                new BigDecimal("499.00"), "http://image.com/ms.png", 10
        );
        when(productRepository.existsBySku("ELEC-MS-001")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(request, 10L));
    }

    @Test
    @DisplayName("Update product with category update and inventory creation")
    void testUpdateProduct_WithCategoryAndStock() {
        Category newCat = new Category("Audio", "audio", "Audio gear", null);
        newCat.setId(2L);

        testProduct.setInventory(null); // test null inventory branch

        ProductUpdateRequestDTO request = new ProductUpdateRequestDTO(
                "Wireless Headset", 2L, "Updated desc", new BigDecimal("7999.00"),
                "http://img.com/headset.png", 30, true
        );

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO updated = productService.updateProduct(100L, request, 10L);

        assertNotNull(updated);
        assertEquals(30, updated.getAvailableStock());
        assertEquals(2L, updated.getCategoryId());
    }
}

