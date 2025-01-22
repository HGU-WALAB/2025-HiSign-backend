package com.example.backend.auth.exception.controller;


import com.example.backend.auth.exception.FailedHisnetLoginException;
import com.example.backend.base.response.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionController {
    @ExceptionHandler(FailedHisnetLoginException.class)
    public ResponseEntity<ExceptionResponse> handleFailedHisnetLoginException(FailedHisnetLoginException e) {
        ExceptionResponse response = ExceptionResponse.builder()
                .error(e.getStatus().toString())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
