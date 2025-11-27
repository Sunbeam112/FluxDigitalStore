package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.exception.InventoryNotFound;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.Inventory;
import com.artemhontar.fluxdigitalstore.repo.InventoryRepository;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final BookService bookService;
    private final InventoryRepository inventoryRepository;
    private final LogDispatchService logDispatchService;

    public InventoryService(BookService bookService, InventoryRepository inventoryRepository, LogDispatchService logDispatchService) {
        this.bookService = bookService;
        this.inventoryRepository = inventoryRepository;
        this.logDispatchService = logDispatchService;
    }


    public int getAvailableStock(Long bookID) {
        Inventory inventory = inventoryRepository.findById(bookID)
                .orElseThrow(() -> new InventoryNotFound("Inventory not found for Book ID: " + bookID));
        return inventory.getWarehouseStock();
    }

    @Transactional
    public Inventory reserveStock(Long bookId, int quantity, Long orderId) {
        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseThrow(() -> new InventoryNotFound("Inventory not found for Book ID: " + bookId));

        if (inventory.getWarehouseStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock available for book ID: " + bookId);
        }

        inventory.setWarehouseStock(inventory.getWarehouseStock() - quantity);
        inventory.setOnHoldStock(inventory.getOnHoldStock() + quantity);

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void dispatchStock(Long bookId, int quantity, Long orderId, Long deliveryAddressId) {
        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseThrow(() -> new InventoryNotFound("Inventory not found for Book ID: " + bookId));

        if (inventory.getOnHoldStock() < quantity) {
            throw new NotEnoughStock("Insufficient ON_HOLD stock for book ID: " + bookId);
        }

        Book book = inventory.getBook();
        logDispatchService.logDispatch(book, quantity, orderId, deliveryAddressId);

        inventory.setOnHoldStock(inventory.getOnHoldStock() - quantity);

        inventoryRepository.save(inventory);
    }


}
