package com.openmarket.exception;

public class AppAlreadyExistException extends RuntimeException {
    public AppAlreadyExistException(String message) {
        super(message);
    }
}
