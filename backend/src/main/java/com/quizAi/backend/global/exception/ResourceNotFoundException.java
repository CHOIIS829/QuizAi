package com.quizAi.backend.global.exception;

public class ResourceNotFoundException extends BaseException {

    private final int statusCode;

    public ResourceNotFoundException(String message, String errorCode) {
        super(message, errorCode);
        this.statusCode = 404;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
