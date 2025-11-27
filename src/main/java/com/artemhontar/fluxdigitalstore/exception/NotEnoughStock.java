package com.artemhontar.fluxdigitalstore.exception;

public class NotEnoughStock extends RuntimeException {
    public NotEnoughStock(String message) {
        super(message);
    }

    
    public NotEnoughStock(String message, Throwable cause) {
        super(message, cause);
    }
}
