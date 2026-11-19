package com.eshopping.product.service;

import com.eshopping.product.dto.CategoryCreateRequestDTO;
import com.eshopping.product.dto.CategoryDTO;
import com.eshopping.product.entity.Category;
import com.eshopping.product.exception.DuplicateResourceException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.repository.CategoryRepository;
import com.eshopping.product.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category("Electronics", "electronics", "Gadgets and tech", "http://image.com/elec.png");
        testCategory.setId(1L);
    }

    @Test
    @DisplayName("Get all categories returns list of DTOs")
    void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(testCategory));

        List<CategoryDTO> categories = categoryService.getAllCategories();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("Electronics", categories.get(0).getName());
    }

    @Test
    @DisplayName("Get category by ID when found returns DTO")
    void testGetCategoryById_Found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        CategoryDTO dto = categoryService.getCategoryById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Electronics", dto.getName());
    }

    @Test
    @DisplayName("Get category by ID when not found throws ResourceNotFoundException")
    void testGetCategoryById_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(999L));
    }

    @Test
    @DisplayName("Create category successfully returns saved DTO")
    void testCreateCategory_Success() {
        CategoryCreateRequestDTO request = new CategoryCreateRequestDTO("Books", "books", "Read books", "http://image.com/books.png");

        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.existsBySlug("books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryDTO created = categoryService.createCategory(request);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("Books", created.getName());
        assertEquals("books", created.getSlug());
    }

    @Test
    @DisplayName("Create category with existing name throws DuplicateResourceException")
    void testCreateCategory_DuplicateName() {
        CategoryCreateRequestDTO request = new CategoryCreateRequestDTO("Electronics", "new-slug", null, null);
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create category with existing slug throws DuplicateResourceException")
    void testCreateCategory_DuplicateSlug() {
        CategoryCreateRequestDTO request = new CategoryCreateRequestDTO("New Name", "electronics", null, null);
        when(categoryRepository.existsByName("New Name")).thenReturn(false);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get category by slug when found returns DTO")
    void testGetCategoryBySlug_Found() {
        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(testCategory));

        CategoryDTO dto = categoryService.getCategoryBySlug("electronics");

        assertNotNull(dto);
        assertEquals("electronics", dto.getSlug());
    }

    @Test
    @DisplayName("Get category by slug when not found throws ResourceNotFoundException")
    void testGetCategoryBySlug_NotFound() {
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryBySlug("unknown"));
    }
}

