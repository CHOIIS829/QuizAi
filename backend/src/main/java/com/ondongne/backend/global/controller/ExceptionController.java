package com.ondongne.backend.global.controller;

import com.ondongne.backend.global.exception.BaseException;
import com.ondongne.backend.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
