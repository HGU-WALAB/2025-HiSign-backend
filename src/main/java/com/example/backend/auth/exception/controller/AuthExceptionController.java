package com.example.backend.auth.exception.controller;


import com.example.backend.auth.exception.FailedHisnetLoginException;
import com.example.backend.auth.exception.MailTimeoutException;
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

    // 🔥 메일 타임아웃 전용 핸들러 추가
    @ExceptionHandler(MailTimeoutException.class)
    public ResponseEntity<ExceptionResponse> handleMailTimeoutException(MailTimeoutException e) {
        ExceptionResponse response = ExceptionResponse.builder()
                .error(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase())  // "Gateway Timeout"
                .message("메일 서버 응답이 지연되어 작업이 실패했습니다. 잠시 후 다시 시도해 주세요.")
                .build();
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response); // 504
    }
}
