package com.ondongne.backend.global.exception;

public class FailCrawlException extends BaseException {

    private static final String MESSAGE = "크롤링에 실패했습니다.";
    private static final String ERROR_CODE = "FAIL_CRAWL";

    public FailCrawlException() {
        super(MESSAGE, ERROR_CODE);
    }

    @Override
    public int getStatusCode() {
        return 500;
    }
}
