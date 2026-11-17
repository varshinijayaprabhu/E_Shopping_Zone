package com.eshopping.product.service;

import com.eshopping.product.dto.InventoryReleaseRequestDTO;
import com.eshopping.product.dto.InventoryReleaseResponseDTO;
import com.eshopping.product.dto.InventoryReserveRequestDTO;
import com.eshopping.product.dto.InventoryReservationResponseDTO;
import com.eshopping.product.entity.Inventory;
import com.eshopping.product.exception.InsufficientStockException;
import com.eshopping.product.exception.ResourceNotFoundException;
import com.eshopping.product.repository.InventoryRepository;
import com.eshopping.product.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setAvailableStock(20);
        testInventory.setReservedStock(5);
        testInventory.setVersion(1L);
    }

    @Test
    @DisplayName("Reserve inventory: Sufficient stock successfully transitions to reserved stock")
    void testReserveInventory_Success() {
        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryReserveRequestDTO request = new InventoryReserveRequestDTO(
                "ORD-TEST-001",
                Collections.singletonList(new InventoryReserveRequestDTO.ItemReservation(100L, 3))
        );

        InventoryReservationResponseDTO response = inventoryService.reserveInventory(request);

        assertNotNull(response);
        assertEquals("ORD-TEST-001", response.getOrderNumber());
        assertEquals(1, response.getReservedItems().size());
        assertEquals(17, response.getReservedItems().get(0).getRemainingStock());
        assertEquals(17, testInventory.getAvailableStock());
        assertEquals(8, testInventory.getReservedStock());
    }

    @Test
    @DisplayName("Reserve inventory: Insufficient stock throws InsufficientStockException and aborts")
    void testReserveInventory_InsufficientStock() {
        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(testInventory));

        InventoryReserveRequestDTO request = new InventoryReserveRequestDTO(
                "ORD-TEST-002",
                Collections.singletonList(new InventoryReserveRequestDTO.ItemReservation(100L, 50)) // requests 50, but only 20 available!
        );

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveInventory(request));
        verify(inventoryRepository, never()).save(any());
        assertEquals(20, testInventory.getAvailableStock());
    }

    @Test
    @DisplayName("Reserve inventory: Non-existent product throws ResourceNotFoundException")
    void testReserveInventory_ProductNotFound() {
        when(inventoryRepository.findByProductIdForUpdate(999L)).thenReturn(Optional.empty());

        InventoryReserveRequestDTO request = new InventoryReserveRequestDTO(
                "ORD-TEST-003",
                Collections.singletonList(new InventoryReserveRequestDTO.ItemReservation(999L, 1))
        );

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.reserveInventory(request));
    }

    @Test
    @DisplayName("Release inventory: Compensating rollback releases reserved stock back to available stock")
    void testReleaseInventory_Success() {
        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryReleaseRequestDTO request = new InventoryReleaseRequestDTO(
                "ORD-TEST-004",
                Collections.singletonList(new InventoryReleaseRequestDTO.ItemRelease(100L, 3))
        );

        InventoryReleaseResponseDTO response = inventoryService.releaseInventory(request);

        assertNotNull(response);
        assertEquals("ORD-TEST-004", response.getOrderNumber());
        assertEquals(1, response.getReleasedItems().size());
        assertEquals(23, response.getReleasedItems().get(0).getAvailableStock());
        assertEquals(23, testInventory.getAvailableStock());
        assertEquals(2, testInventory.getReservedStock());
    }
}

