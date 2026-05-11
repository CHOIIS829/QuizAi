package com.quizAi.backend.global.exception;

public class DuplicateNicknameException extends BaseException {

    private static final String MESSAGE = "이미 사용 중인 닉네임입니다.";
    private static final String ERROR_CODE = "DUPLICATE_NICKNAME";

    public DuplicateNicknameException() {
        super(MESSAGE, ERROR_CODE);
    }

    @Override
    public int getStatusCode() {
        return 409;
    }
}
