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

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(BaseException e) {
        log.error(">>>>> [ERROR] code : {}, message : {}", e.getStatusCode(), e.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .code(e.getStatusCode())
                .message(e.getMessage())
                .errorCode(e.getErrorCode())
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(body);
    }
}
