package com.ondongne.backend.global.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public abstract int getStatusCode();
}
