package com.quizAi.backend.global.exception;

public class RateLimitExceededException extends BaseException {

    private static final String MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    public RateLimitExceededException() {
        super(MESSAGE, ERROR_CODE);
    }

    @Override
    public int getStatusCode() {
        return 429;
    }
}
