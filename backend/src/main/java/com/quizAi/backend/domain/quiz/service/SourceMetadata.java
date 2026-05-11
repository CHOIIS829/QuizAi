package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.quiz.entity.SourceType;

public record SourceMetadata(
        SourceType sourceType,
        String sourceHost
) {
}
