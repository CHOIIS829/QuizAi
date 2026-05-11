package com.quizAi.backend.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicTagDto {

    private String slug;
    private String displayName;
}
