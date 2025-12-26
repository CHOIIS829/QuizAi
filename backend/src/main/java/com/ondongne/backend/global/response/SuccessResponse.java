package com.ondongne.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class SuccessResponse<T> {

    int code;
    String message;
    T data;

}
