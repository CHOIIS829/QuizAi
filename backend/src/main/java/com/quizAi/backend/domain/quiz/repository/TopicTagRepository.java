package com.quizAi.backend.domain.quiz.repository;

import com.quizAi.backend.domain.quiz.entity.TopicTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicTagRepository extends JpaRepository<TopicTag, Long> {

    Optional<TopicTag> findBySlug(String slug);

    List<TopicTag> findBySlugIn(Collection<String> slugs);
}
