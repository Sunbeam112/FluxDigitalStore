package com.artemhontar.fluxdigitalstore.exception;

public class InventoryNotFound extends RuntimeException {
    public InventoryNotFound(String message) {
        super(message);
    }

    public InventoryNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}
