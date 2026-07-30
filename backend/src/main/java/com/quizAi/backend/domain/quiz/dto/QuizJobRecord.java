package com.quizAi.backend.domain.quiz.dto;

import com.quizAi.backend.domain.quiz.entity.SourceType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizJobRecord {

    private String jobId;
    private QuizResponseDto.JobStatus status;
    private QuizResultDto result;
    private String message;
    private Long persistedQuizId;
    private Long ownerUserId;
    private boolean persistResult;
    private String sourceUrl;
    private SourceType sourceType;
    private String sourceHost;
    private Instant createdAt;

    public QuizResponseDto toResponseDto() {
        return QuizResponseDto.builder()
                .jobId(jobId)
                .status(status)
                .result(result)
                .message(message)
                .persistedQuizId(persistedQuizId)
                .build();
    }
}
