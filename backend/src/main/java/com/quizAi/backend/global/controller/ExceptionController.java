package com.quizAi.backend.global.controller;

import com.quizAi.backend.global.exception.BaseException;
import com.quizAi.backend.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.error(">>>>> [ERROR] code : {}, message : {}", e.getStatusCode(), e.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .code(e.getStatusCode())
                .message(e.getMessage())
                .errorCode(e.getErrorCode())
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));

        ErrorResponse body = ErrorResponse.builder()
                .code(400)
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse body = ErrorResponse.builder()
                .code(403)
                .message("접근 권한이 없습니다.")
                .errorCode("ACCESS_DENIED")
                .build();

        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e) {
        log.error(">>>>> [ERROR] message : {}", e.getMessage(), e);

        ErrorResponse body = ErrorResponse.builder()
                .code(500)
                .message("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .errorCode("INTERNAL_SERVER_ERROR")
                .build();

        return ResponseEntity.status(500).body(body);
    }
}
