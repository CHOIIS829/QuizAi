package com.ondongne.backend.global.exception;

public class FailDownloadException extends BaseException {

    private static final String MESSAGE = "영상 다운로드에 실패했습니다.";
    private static final String ERROR_CODE = "FAIL_DOWNLOAD";

    public FailDownloadException() {
        super(MESSAGE, ERROR_CODE);
    }

    @Override
    public int getStatusCode() {
        return 500;
    }
}
