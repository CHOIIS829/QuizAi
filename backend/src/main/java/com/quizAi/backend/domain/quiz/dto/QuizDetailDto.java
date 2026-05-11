package com.quizAi.backend.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizDetailDto {

    private QuizListItemDto quiz;
    private QuizResultDto quizResult;
    private List<TopicTagDto> topicTags;
}
