package com.eshopping.order.client;

import com.eshopping.order.dto.client.InventoryReleaseClientRequestDTO;
import com.eshopping.order.dto.client.InventoryReserveClientRequestDTO;
import com.eshopping.order.dto.client.InventoryReserveClientResponseDTO;
import com.eshopping.order.dto.client.ProductClientDTO;

public interface ProductServiceClient {

    InventoryReserveClientResponseDTO reserveStock(InventoryReserveClientRequestDTO request);

    void releaseStock(InventoryReleaseClientRequestDTO request);

    ProductClientDTO getProductById(Long productId);
}

