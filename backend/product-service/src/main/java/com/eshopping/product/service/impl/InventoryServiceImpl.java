package com.eshopping.product.service.impl;

import com.eshopping.product.dto.InventoryReleaseRequestDTO;
import com.eshopping.product.dto.InventoryReleaseResponseDTO;
import com.eshopping.product.dto.InventoryReserveRequestDTO;
import com.eshopping.product.dto.InventoryReservationResponseDTO;
import com.eshopping.product.entity.Inventory;
import com.eshopping.product.exception.InsufficientStockException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.repository.InventoryRepository;
import com.eshopping.product.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final java.util.Map<String, InventoryReservationResponseDTO> activeReservations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<String> releasedOrders = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public InventoryReservationResponseDTO reserveInventory(InventoryReserveRequestDTO request) {
        // Idempotency: prevent double reservations for the same order
        if (activeReservations.containsKey(request.getOrderNumber())) {
            log.info("Idempotent reserve request for order: {}. Returning existing reservation.", request.getOrderNumber());
            return activeReservations.get(request.getOrderNumber());
        }

        log.info("Attempting inventory reservation for order: {}", request.getOrderNumber());

        List<InventoryReservationResponseDTO.ReservedItemResult> results = new ArrayList<>();

        for (InventoryReserveRequestDTO.ItemReservation item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + item.getProductId()));

            if (inventory.getAvailableStock() < item.getQuantity()) {
                log.error("Insufficient stock for product ID {}. Available: {}, Requested: {}",
                        item.getProductId(), inventory.getAvailableStock(), item.getQuantity());
                throw new InsufficientStockException("Product with ID " + item.getProductId() +
                        " only has " + inventory.getAvailableStock() + " available stock, but " + item.getQuantity() + " were requested");
            }

            // Deduct available stock and allocate to reserved stock
            inventory.setAvailableStock(inventory.getAvailableStock() - item.getQuantity());
            inventory.setReservedStock(inventory.getReservedStock() + item.getQuantity());
            inventoryRepository.save(inventory);

            log.info("Reserved {} units for product ID {} on order {}. Remaining available: {}",
                    item.getQuantity(), item.getProductId(), request.getOrderNumber(), inventory.getAvailableStock());

            results.add(new InventoryReservationResponseDTO.ReservedItemResult(
                    item.getProductId(),
                    item.getQuantity(),
                    inventory.getAvailableStock()
            ));
        }

        InventoryReservationResponseDTO response = new InventoryReservationResponseDTO(request.getOrderNumber(), results);
        activeReservations.put(request.getOrderNumber(), response);
        return response;
    }

    @Override
    public InventoryReleaseResponseDTO releaseInventory(InventoryReleaseRequestDTO request) {
        // Idempotency: prevent releasing multiple times for the same order
        if (releasedOrders.contains(request.getOrderNumber())) {
            log.warn("Order {} has already been released. Idempotent skip.", request.getOrderNumber());
            return new InventoryReleaseResponseDTO(request.getOrderNumber(), Collections.emptyList());
        }

        log.info("Releasing reserved inventory for order: {}", request.getOrderNumber());

        List<InventoryReleaseResponseDTO.ReleasedItemResult> results = new ArrayList<>();

        for (InventoryReleaseRequestDTO.ItemRelease item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + item.getProductId()));

            int releaseQty = Math.min(item.getQuantity(), inventory.getReservedStock());
            inventory.setReservedStock(inventory.getReservedStock() - releaseQty);
            inventory.setAvailableStock(inventory.getAvailableStock() + releaseQty);
            inventoryRepository.save(inventory);

            log.info("Released {} reserved units back to available stock for product ID {} on order {}. New available: {}",
                    releaseQty, item.getProductId(), request.getOrderNumber(), inventory.getAvailableStock());

            results.add(new InventoryReleaseResponseDTO.ReleasedItemResult(
                    item.getProductId(),
                    releaseQty,
                    inventory.getAvailableStock()
            ));
        }

        releasedOrders.add(request.getOrderNumber());
        activeReservations.remove(request.getOrderNumber());
        return new InventoryReleaseResponseDTO(request.getOrderNumber(), results);
    }
}

