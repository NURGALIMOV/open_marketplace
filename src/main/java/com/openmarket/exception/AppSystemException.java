package com.openmarket.exception;

public class AppSystemException extends RuntimeException {
    public AppSystemException() {
    }

    public AppSystemException(String message) {
        super(message);
    }

    public AppSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppSystemException(Throwable cause) {
        super(cause);
    }

    public AppSystemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
