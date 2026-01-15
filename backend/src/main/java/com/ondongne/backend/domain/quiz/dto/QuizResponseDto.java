package com.ondongne.backend.domain.quiz.dto;

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
    private String errorMessage;

    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

}
