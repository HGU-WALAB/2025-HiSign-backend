package com.example.backend.auth.exception;

public class DoNotExistException extends RuntimeException {
  public DoNotExistException(String message) {
    super(message);
  }
}
