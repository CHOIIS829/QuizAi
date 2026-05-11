package com.quizAi.backend.domain.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizRequestDto {

    @NotBlank(message = "URL을 입력해주세요.")
    private String url;

    @Min(value = 1, message = "문제 수는 1개 이상이어야 합니다.")
    @Max(value = 20, message = "문제 수는 20개 이하여야 합니다.")
    private int quizCount;
}
