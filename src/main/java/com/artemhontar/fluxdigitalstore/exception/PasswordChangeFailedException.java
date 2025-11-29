package com.artemhontar.fluxdigitalstore.exception;

public class PasswordChangeFailedException extends RuntimeException {

    /**
     * Constructs a new PasswordChangeFailedException with the specified detail message.
     * * @param message The detail message (e.g., "PASSWORD_CHANGE_FAILED").
     */
    public PasswordChangeFailedException(String message) {
        super(message);
    }

    /**
     * Constructs a new PasswordChangeFailedException with no detail message.
     */
    public PasswordChangeFailedException() {
        super("Password change operation failed due to an internal error.");
    }
}
