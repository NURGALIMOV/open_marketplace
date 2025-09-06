package com.openmarket.exception;

/**
 * Business logic exception
 */
public class AppBusinessException extends RuntimeException {
    
    public AppBusinessException(String message) {
        super(message);
    }
    
    public AppBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
