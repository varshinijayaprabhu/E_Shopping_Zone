package com.eshopping.product.service;

import com.eshopping.product.dto.CategoryCreateRequestDTO;
import com.eshopping.product.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryDTO> getAllCategories();

    CategoryDTO getCategoryById(Long id);

    CategoryDTO getCategoryBySlug(String slug);

    CategoryDTO createCategory(CategoryCreateRequestDTO request);
}

