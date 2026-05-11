package com.quizAi.backend.domain.quiz.dto;

import com.quizAi.backend.domain.quiz.entity.SourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class QuizListItemDto {

    private Long id;
    private String title;
    private String authorNickname;
    private String sourceUrl;
    private SourceType sourceType;
    private String sourceHost;
    private List<TopicTagDto> topicTags;
    private LocalDateTime publishedAt;
    private int questionCount;
}
