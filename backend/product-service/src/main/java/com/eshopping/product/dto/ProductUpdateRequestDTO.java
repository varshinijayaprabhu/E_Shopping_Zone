package com.eshopping.product.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductUpdateRequestDTO {

    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private Long categoryId;

    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @Size(max = 2000000, message = "Image URL or data must not exceed 2MB")
    private String imageUrl;

    @Min(value = 0, message = "Available stock cannot be negative")
    private Integer availableStock;

    private Boolean active;

    public ProductUpdateRequestDTO() {
    }

    public ProductUpdateRequestDTO(String name, Long categoryId, String description,
                                   BigDecimal price, String imageUrl, Integer availableStock, Boolean active) {
        this.name = name;
        this.categoryId = categoryId;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.availableStock = availableStock;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

