package com.eshopping.product.service.impl;

import com.eshopping.product.dto.CategoryCreateRequestDTO;
import com.eshopping.product.dto.CategoryDTO;
import com.eshopping.product.entity.Category;
import com.eshopping.product.exception.DuplicateResourceException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.repository.CategoryRepository;
import com.eshopping.product.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found"));
        return mapToDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category with slug '" + slug + "' not found"));
        return mapToDTO(category);
    }

    @Override
    public CategoryDTO createCategory(CategoryCreateRequestDTO request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
        }

        Category category = new Category(
                request.getName(),
                request.getSlug().toLowerCase().trim(),
                request.getDescription(),
                request.getImageUrl()
        );

        Category saved = categoryRepository.save(category);
        log.info("Created new category: '{}' (ID: {})", saved.getName(), saved.getId());
        return mapToDTO(saved);
    }

    private CategoryDTO mapToDTO(Category c) {
        return new CategoryDTO(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                c.getImageUrl()
        );
    }
}

