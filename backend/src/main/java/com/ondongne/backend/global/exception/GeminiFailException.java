package com.ondongne.backend.global.exception;

public class GeminiFailException extends BaseException {

    private static final String MESSAGE = "Gemini 서비스 오류가 발생했습니다.";
    private static final String ERROR_CODE = "GEMINI_FAIL_ERROR";

    public GeminiFailException() {
        super(MESSAGE, ERROR_CODE);
    }

    public GeminiFailException(Throwable cause) {
        super(MESSAGE, ERROR_CODE, cause);
    }

    @Override
    public int getStatusCode() {
        return 500;
    }
}
