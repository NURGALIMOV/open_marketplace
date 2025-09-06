package com.openmarket.exception;

/**
 * Exception for Ozon API errors
 */
public class OzonApiException extends RuntimeException {
    public OzonApiException(String message) {
        super(message);
    }

    public OzonApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
