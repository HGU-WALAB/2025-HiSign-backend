package com.example.backend.auth.exception;

public class MailTimeoutException extends RuntimeException {
    public MailTimeoutException(String message) {
        super(message);
    }
    public MailTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
