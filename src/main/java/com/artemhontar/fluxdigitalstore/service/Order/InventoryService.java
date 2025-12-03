package com.artemhontar.fluxdigitalstore.service.Order;

import com.artemhontar.fluxdigitalstore.exception.InventoryNotFound;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.Inventory;
import com.artemhontar.fluxdigitalstore.model.repo.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for managing inventory stock levels,
 * including checking available stock, reserving stock for pending orders,
 * and dispatching reserved stock upon order fulfillment.
 */
@Service
@Slf4j // Add Lombok logging annotation
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LogDispatchService logDispatchService;

    /**
     * Constructs the InventoryService with required dependencies.
     *
     * @param inventoryRepository The repository for managing {@link Inventory} entities.
     * @param logDispatchService  The service for logging dispatch events.
     */
    public InventoryService(InventoryRepository inventoryRepository, LogDispatchService logDispatchService) {
        this.inventoryRepository = inventoryRepository;
        this.logDispatchService = logDispatchService;
    }


    /**
     * Retrieves the current available stock (Warehouse Stock) for a specific book.
     *
     * @param bookID The ID of the book/inventory item.
     * @return The quantity of items available in the warehouse.
     * @throws InventoryNotFound If no inventory record exists for the given book ID.
     */
    public int getAvailableStock(Long bookID) {
        log.info("Checking available stock for Book ID: {}", bookID);
        Inventory inventory = inventoryRepository.findById(bookID)
                .orElseThrow(() -> {
                    log.error("Inventory not found for Book ID: {}", bookID);
                    return new InventoryNotFound("Inventory not found for Book ID: " + bookID);
                });
        log.debug("Available stock for Book ID {}: {}", bookID, inventory.getWarehouseStock());
        return inventory.getWarehouseStock();
    }

    /**
     * Reserves a specific quantity of stock for a new order.
     * This moves the quantity from {@code warehouseStock} to {@code onHoldStock}.
     *
     * @param bookId   The ID of the book/inventory item.
     * @param quantity The amount of stock to reserve.
     * @param orderId  The ID of the order making the reservation.
     * @return The updated {@link Inventory} entity.
     * @throws InventoryNotFound        If no inventory record exists for the given book ID.
     * @throws IllegalArgumentException If the warehouse stock is insufficient.
     */
    @Transactional
    public Inventory reserveStock(Long bookId, int quantity, Long orderId) {
        log.info("Attempting to reserve {} units of stock for Book ID {} (Order ID: {})", quantity, bookId, orderId);
        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseThrow(() -> {
                    log.error("Reservation failed: Inventory not found for Book ID: {}", bookId);
                    return new InventoryNotFound("Inventory not found for Book ID: " + bookId);
                });

        if (inventory.getWarehouseStock() < quantity) {
            log.warn("Reservation failed for Book ID {}: Insufficient stock. Available: {}, Requested: {}",
                    bookId, inventory.getWarehouseStock(), quantity);
            throw new IllegalArgumentException("Insufficient stock available for book ID: " + bookId);
        }

        inventory.setWarehouseStock(inventory.getWarehouseStock() - quantity);
        inventory.setOnHoldStock(inventory.getOnHoldStock() + quantity);

        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Stock reserved successfully. Book ID {}. New Warehouse Stock: {}, New On-Hold Stock: {}",
                bookId, savedInventory.getWarehouseStock(), savedInventory.getOnHoldStock());
        return savedInventory;
    }

    /**
     * Dispatches a specific quantity of stock, fulfilling a reserved order.
     * This decreases the {@code onHoldStock} and logs the dispatch event.
     *
     * @param bookId            The ID of the book/inventory item.
     * @param quantity          The amount of stock to dispatch.
     * @param orderId           The ID of the order being dispatched.
     * @param deliveryAddressId The ID of the delivery address.
     * @throws InventoryNotFound If no inventory record exists for the given book ID.
     * @throws NotEnoughStock    If the on-hold stock is insufficient (a critical error after reservation).
     */
    @Transactional
    public void dispatchStock(Long bookId, int quantity, Long orderId, Long deliveryAddressId) {
        log.info("Attempting to dispatch {} units of stock for Book ID {} (Order ID: {})", quantity, bookId, orderId);
        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseThrow(() -> {
                    log.error("Dispatch failed: Inventory not found for Book ID: {}", bookId);
                    return new InventoryNotFound("Inventory not found for Book ID: " + bookId);
                });

        if (inventory.getOnHoldStock() < quantity) {
            log.error("Dispatch failed for Book ID {}: Insufficient ON_HOLD stock. Reserved: {}, Requested: {}. This indicates a critical inventory inconsistency.",
                    bookId, inventory.getOnHoldStock(), quantity);
            throw new NotEnoughStock("Insufficient ON_HOLD stock for book ID: " + bookId);
        }

        Book book = inventory.getBook();
        log.debug("Logging dispatch event before updating inventory.");
        logDispatchService.logDispatch(book, quantity, orderId, deliveryAddressId);

        inventory.setOnHoldStock(inventory.getOnHoldStock() - quantity);

        inventoryRepository.save(inventory);
        log.info("Dispatch successful. Book ID {}. Remaining On-Hold Stock: {}",
                bookId, inventory.getOnHoldStock());
    }
}