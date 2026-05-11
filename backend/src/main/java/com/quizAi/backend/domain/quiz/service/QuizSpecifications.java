package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.quiz.entity.Quiz;
import com.quizAi.backend.domain.quiz.entity.QuizVisibility;
import com.quizAi.backend.domain.quiz.entity.SourceType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

public final class QuizSpecifications {

    private QuizSpecifications() {
    }

    public static Specification<Quiz> ownerIdEquals(Long ownerUserId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("ownerUser").get("id"), ownerUserId);
    }

    public static Specification<Quiz> visibilityEquals(QuizVisibility visibility) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("visibility"), visibility);
    }

    public static Specification<Quiz> sourceTypeEquals(SourceType sourceType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("sourceType"), sourceType);
    }

    public static Specification<Quiz> hasTag(String tagSlug) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.equal(root.join("topicTags", JoinType.LEFT).get("slug"), tagSlug);
        };
    }
}
