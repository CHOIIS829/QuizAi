package com.quizAi.backend.domain.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GuestQuizImportRequestDto {

    @NotBlank(message = "원본 URL이 필요합니다.")
    private String sourceUrl;

    @Valid
    @NotNull(message = "퀴즈 결과가 필요합니다.")
    private QuizResultDto quizResult;
}
