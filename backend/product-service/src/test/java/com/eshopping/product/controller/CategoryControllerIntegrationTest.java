package com.eshopping.product.controller;

import com.eshopping.product.dto.CategoryCreateRequestDTO;
import com.eshopping.product.dto.CategoryDTO;
import com.eshopping.product.security.JwtTokenProvider;
import com.eshopping.product.service.CategoryService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/categories: Public retrieval returns 200 OK")
    void testGetAllCategories() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Electronics", "electronics", "Electronic devices", "http://img.com/e.png");
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/categories/{id}: Public retrieval returns 200 OK")
    void testGetCategoryById() throws Exception {
        CategoryDTO dto = new CategoryDTO(2L, "Books", "books", "Read books", null);
        when(categoryService.getCategoryById(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("books"));
    }

    @Test
    @DisplayName("GET /api/categories/slug/{slug}: Public retrieval returns 200 OK")
    void testGetCategoryBySlug() throws Exception {
        CategoryDTO dto = new CategoryDTO(3L, "Fashion", "fashion", "Clothing", null);
        when(categoryService.getCategoryBySlug("fashion")).thenReturn(dto);

        mockMvc.perform(get("/api/categories/slug/fashion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Fashion"));
    }

    @Test
    @DisplayName("POST /api/categories: Merchant role creates category and returns 201 Created")
    void testCreateCategory_MerchantRole() throws Exception {
        String merchantJwt = jwtTokenProvider.generateTokenForTest(10L, "merchant@example.com", Collections.singleton("ROLE_MERCHANT"));
        CategoryCreateRequestDTO request = new CategoryCreateRequestDTO("Sports", "sports", "Sporting goods", null);
        CategoryDTO created = new CategoryDTO(4L, "Sports", "sports", "Sporting goods", null);

        when(categoryService.createCategory(any())).thenReturn(created);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + merchantJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.name").value("Sports"));
    }
}
