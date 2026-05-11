package com.quizAi.backend.domain.quiz.repository;

import com.quizAi.backend.domain.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {

    @EntityGraph(attributePaths = {"ownerUser", "topicTags"})
    @Override
    org.springframework.data.domain.Page<Quiz> findAll(org.springframework.data.jpa.domain.Specification<Quiz> spec, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"ownerUser", "topicTags", "questions"})
    @Query("select q from Quiz q where q.id = :quizId")
    Optional<Quiz> findDetailById(@Param("quizId") Long quizId);
}
