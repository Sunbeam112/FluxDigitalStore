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

    @BeforeEach
    void setUp() {
        // Create a mock Book entity for use within Inventory
        mockBook = Book.builder()
                .id(BOOK_ID)
                .isbn("1234567890123")
                .title("Test Book Title")
                .build();

        // Create a mock Inventory with initial stock levels
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

    @Test
    void getAvailableStock_InventoryFound_ReturnsWarehouseStock() {
        // Arrange
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        int expectedStock = mockInventory.getWarehouseStock();

        // Act
        int actualStock = inventoryService.getAvailableStock(BOOK_ID);

        // Assert
        assertEquals(expectedStock, actualStock);
        verify(inventoryRepository, times(1)).findById(BOOK_ID);
    }

    @Test
    void getAvailableStock_InventoryNotFound_ThrowsException() {
        // Arrange
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        InventoryNotFound exception = assertThrows(InventoryNotFound.class, () -> {
            inventoryService.getAvailableStock(BOOK_ID);
        });

        // FIX: Ensure the message is not null before checking its content
        assertNotNull(exception.getMessage(), "Exception message should not be null.");
        assertTrue(exception.getMessage().contains("Inventory not found for Book ID: " + BOOK_ID));
    }

    // ===================================
    // TEST: reserveStock(Long bookId, int quantity, Long orderId)
    // ===================================

    @Test
    void reserveStock_SufficientStock_ReservesAndSavesInventory() {
        // Arrange
        int reserveQuantity = 3;
        int initialWarehouseStock = mockInventory.getWarehouseStock(); // 10
        int initialOnHoldStock = mockInventory.getOnHoldStock();       // 5

        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        // Act
        Inventory result = inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);

        // Assert
        assertEquals(initialWarehouseStock - reserveQuantity, result.getWarehouseStock()); // 10 - 3 = 7
        assertEquals(initialOnHoldStock + reserveQuantity, result.getOnHoldStock());     // 5 + 3 = 8
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    @Test
    void reserveStock_InventoryNotFound_ThrowsException() {
        // Arrange
        int reserveQuantity = 3;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InventoryNotFound.class, () -> {
            inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);
        });

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveStock_InsufficientStock_ThrowsIllegalArgumentException() {
        // Arrange
        int reserveQuantity = 11; // More than available stock (10)
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.reserveStock(BOOK_ID, reserveQuantity, ORDER_ID);
        });

        // The IllegalArgumentException message should contain the expected text
        assertTrue(exception.getMessage().contains("Insufficient stock available for book ID: " + BOOK_ID));
        verify(inventoryRepository, never()).save(any());
    }

    // ===================================
    // TEST: dispatchStock(Long bookId, int quantity, Long orderId, Long deliveryAddressId)
    // ===================================

    @Test
    void dispatchStock_SufficientOnHoldStock_DispatchesAndSavesInventory() {
        // Arrange
        int dispatchQuantity = 5;
        int initialOnHoldStock = mockInventory.getOnHoldStock(); // 5

        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));
        doNothing().when(logDispatchService).logDispatch(any(Book.class), anyInt(), anyLong(), anyLong());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        // Act
        inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);

        // Assert
        // The onHoldStock should now be 0 (5 - 5)
        assertEquals(initialOnHoldStock - dispatchQuantity, mockInventory.getOnHoldStock());

        // Verify LogDispatchService was called with correct parameters
        verify(logDispatchService, times(1)).logDispatch(mockBook, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);

        // Verify repository save was called
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    @Test
    void dispatchStock_InventoryNotFound_ThrowsException() {
        // Arrange
        int dispatchQuantity = 1;
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InventoryNotFound.class, () -> {
            inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);
        });

        verify(logDispatchService, never()).logDispatch(any(), anyInt(), anyLong(), anyLong());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void dispatchStock_InsufficientOnHoldStock_ThrowsNotEnoughStockException() {
        // Arrange
        int dispatchQuantity = 6; // More than available on-hold stock (5)
        when(inventoryRepository.findById(BOOK_ID)).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        NotEnoughStock exception = assertThrows(NotEnoughStock.class, () -> {
            inventoryService.dispatchStock(BOOK_ID, dispatchQuantity, ORDER_ID, DELIVERY_ADDRESS_ID);
        });

        // FIX: Ensure the message is not null before checking its content
        assertNotNull(exception.getMessage(), "Exception message should not be null.");
        assertTrue(exception.getMessage().contains("Insufficient ON_HOLD stock for book ID: " + BOOK_ID));
        verify(logDispatchService, never()).logDispatch(any(), anyInt(), anyLong(), anyLong());
        verify(inventoryRepository, never()).save(any());
    }
}