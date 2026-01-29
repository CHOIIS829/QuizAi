package com.quizAi.backend.global.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    int code;
    String message;
    String errorCode;
}
