package com.ondongne.backend.global.exception;

public class geminiFailException extends BaseException {

    private static final String MESSAGE = "Gemini 서비스 오류가 발생했습니다.";
    private static final String ERROR_CODE = "GEMINI_FAIL_ERROR";

    public geminiFailException() {
        super(MESSAGE, ERROR_CODE);
    }

    @Override
    public int getStatusCode() {
        return 500;
    }
}
