package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.quiz.entity.TopicTag;
import com.quizAi.backend.domain.quiz.entity.TopicTagCatalog;
import com.quizAi.backend.domain.quiz.repository.TopicTagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TopicTagInitializer implements ApplicationRunner {

    private final TopicTagRepository topicTagRepository;

    public TopicTagInitializer(TopicTagRepository topicTagRepository) {
        this.topicTagRepository = topicTagRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (TopicTagCatalog item : TopicTagCatalog.values()) {
            topicTagRepository.findBySlug(item.getSlug())
                    .orElseGet(() -> topicTagRepository.save(TopicTag.builder()
                            .slug(item.getSlug())
                            .displayName(item.getDisplayName())
                            .build()));
        }
    }
}
