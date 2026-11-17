package com.eshopping.order.client;

import com.eshopping.order.dto.client.CartClientDTO;

public interface CartServiceClient {

    CartClientDTO getCart(Long customerId, String authHeader);

    void clearCart(Long customerId, String authHeader);
}

