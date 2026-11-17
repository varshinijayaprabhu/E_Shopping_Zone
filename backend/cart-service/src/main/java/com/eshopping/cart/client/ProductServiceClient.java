package com.eshopping.cart.client;

import com.eshopping.cart.dto.ProductClientDTO;

public interface ProductServiceClient {

    ProductClientDTO.ProductData getProductDetails(Long productId);
}

