package com.artemhontar.fluxdigitalstore.exception;

public class InvalidTokenException extends RuntimeException {

    /**
     * Constructs a new InvalidTokenException with the specified detail message.
     * * @param message The detail message (e.g., "INVALID_OR_EXPIRED_TOKEN").
     */
    public InvalidTokenException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidTokenException with no detail message.
     */
    public InvalidTokenException() {
        super("Invalid or used token.");
    }
}
