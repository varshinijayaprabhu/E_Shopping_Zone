package com.eshopping.cart.service;

import com.eshopping.cart.dto.AddToCartRequestDTO;
import com.eshopping.cart.dto.CartResponseDTO;
import com.eshopping.cart.dto.UpdateCartItemRequestDTO;

public interface CartService {

    CartResponseDTO getCart(Long customerId);

    CartResponseDTO addItemToCart(Long customerId, AddToCartRequestDTO request);

    CartResponseDTO updateItemQuantity(Long customerId, Long itemId, UpdateCartItemRequestDTO request);

    CartResponseDTO removeItem(Long customerId, Long itemId);

    void clearCart(Long customerId);
}

