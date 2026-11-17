package com.eshopping.product.service;

import com.eshopping.product.dto.InventoryReleaseRequestDTO;
import com.eshopping.product.dto.InventoryReleaseResponseDTO;
import com.eshopping.product.dto.InventoryReserveRequestDTO;
import com.eshopping.product.dto.InventoryReservationResponseDTO;

public interface InventoryService {

    InventoryReservationResponseDTO reserveInventory(InventoryReserveRequestDTO request);

    InventoryReleaseResponseDTO releaseInventory(InventoryReleaseRequestDTO request);
}

