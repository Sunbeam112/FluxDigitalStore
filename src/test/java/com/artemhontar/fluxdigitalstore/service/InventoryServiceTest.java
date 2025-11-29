package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.exception.InventoryNotFound;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.Inventory;
import com.artemhontar.fluxdigitalstore.repo.InventoryRepository;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@code InventoryService} class.
 * These tests ensure the core inventory management operations—checking stock,
 * reserving stock for orders, and dispatching stock—behave correctly, including
 * handling success cases and specific business exceptions like {@code InventoryNotFound}
 * and {@code NotEnoughStock}.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LogDispatchService logDispatchService;

    @InjectMocks
    private InventoryService inventoryService;

    // --- Test Data ---
    private static final Long BOOK_ID = 101L;
    private static final Long ORDER_ID = 500L;
    private static final Long DELIVERY_ADDRESS_ID = 900L;
    private Book mockBook;
    private Inventory mockInventory;

    /**
     * Sets up the necessary mock entities and initial inventory state before each test.
     */
    @BeforeEach
    void setUp() {
        mockBook = Book.builder()
                .id(BOOK_ID)
                .isbn("1234567890123")
                .title("Test Book Title")
                .build();

        mockInventory = Inventory.builder()
                .id(BOOK_ID)
                .book(mockBook)
                .warehouseStock(10)
                .onHoldStock(5)
                .build();
    }

    // ===================================
    // TEST: getAvailableStock(Long bookID)
    // ===================================

    /**
     * Tests that {@code getAvailableStock} returns the correct warehouse stock level
     * when the inventory record is found.
     */
    @Test
    void getAvailableStock_InventoryFound_ReturnsWarehouseStock() {
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        int expectedStock = mockInventory.getWarehouseStock();

        int actualStock = inventoryService.getAvailableStock(BOOK_ID);

        assertEquals(expectedStock, actualStock);
        verify(inventoryRepository, times(1)).findById(BOOK_ID);
    }

    /**
     * Tests that {@code getAvailableStock} throws {@code InventoryNotFound}
     * when the inventory record for the given book ID does not exist.
     */
    @Test
    void getAvailableStock_InventoryNotFound_ThrowsException() {
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        InventoryNotFound exception = assertThrows(InventoryNotFound.class, () -> {
            inventoryService.getAvailableStock(BOOK_ID);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Inventory not found for Book ID: " + BOOK_ID));
    }

    // ===================================
    // TEST: reserveStock(Long bookId, int quantity, Long orderId)
    // ===================================

    /**
     * Tests successful reservation of stock, ensuring {@code warehouseStock} decreases,
     * {@code onHoldStock} increases, and the repository save method is called.
     */
    @Test
    void reserveStock_SufficientStock_ReservesAndSavesInventory() {
        int reserveQuantity = 3;
        int initialWarehouseStock = mockInventory.getWarehouseStock();
        int initialOnHoldStock = mockInventory.getOnHoldStock();

        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        Inventory result = inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);

        assertEquals(initialWarehouseStock - reserveQuantity, result.getWarehouseStock());
        assertEquals(initialOnHoldStock + reserveQuantity, result.getOnHoldStock());
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    /**
     * Tests that {@code reserveStock} throws {@code InventoryNotFound}
     * when the inventory record is missing.
     */
    @Test
    void reserveStock_InventoryNotFound_ThrowsException() {
        int reserveQuantity = 3;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFound.class, () -> {
            inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);
        });

        verify(inventoryRepository, never()).save(any());
    }

    /**
     * Tests that {@code reserveStock} throws {@code IllegalArgumentException}
     * when the requested quantity exceeds the available {@code warehouseStock}.
     */
    @Test
    void reserveStock_InsufficientStock_ThrowsIllegalArgumentException() {
        int reserveQuantity = 11;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock available for book ID: " + BOOK_ID));
        verify(inventoryRepository, never()).save(any());
    }

    // ===================================
    // TEST: dispatchStock(Long bookId, int quantity, Long orderId, Long deliveryAddressId)
    // ===================================

    /**
     * Tests successful dispatch of stock, ensuring {@code onHoldStock} decreases,
     * {@code logDispatchService} is called, and the inventory is saved.
     */
    @Test
    void dispatchStock_SufficientOnHoldStock_DispatchesAndSavesInventory() {
        int dispatchQuantity = 5;
        int initialOnHoldStock = mockInventory.getOnHoldStock();

        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        doNothing().when(logDispatchService).logDispatch(any(Book.class), anyInt(), anyLong(), anyLong());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);

        assertEquals(initialOnHoldStock - dispatchQuantity, mockInventory.getOnHoldStock());

        verify(logDispatchService, times(1)).logDispatch(mockBook, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);

        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    /**
     * Tests that {@code dispatchStock} throws {@code InventoryNotFound}
     * when the inventory record is missing.
     */
    @Test
    void dispatchStock_InventoryNotFound_ThrowsException() {
        int dispatchQuantity = 1;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFound.class, () -> {
            inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);
        });

        verify(logDispatchService, never()).logDispatch(any(), anyInt(), anyLong(), anyLong());
        verify(inventoryRepository, never()).save(any());
    }

    /**
     * Tests that {@code dispatchStock} throws {@code NotEnoughStock}
     * when the requested quantity exceeds the available {@code onHoldStock}.
     */
    @Test
    void dispatchStock_InsufficientOnHoldStock_ThrowsNotEnoughStockException() {
        int dispatchQuantity = 6;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));

        NotEnoughStock exception = assertThrows(NotEnoughStock.class, () -> {
            inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Insufficient ON_HOLD stock for book ID: " + BOOK_ID));
        verify(logDispatchService, never()).logDispatch(any(), anyInt(), anyLong(), anyLong());
        verify(inventoryRepository, never()).save(any());
    }
}