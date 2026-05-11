package com.quizAi.backend.domain.quiz.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponseDto {

    private String jobId;
    private JobStatus status;
    private QuizResultDto result;
    private String message;
    private Long persistedQuizId;

    public enum JobStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

}
