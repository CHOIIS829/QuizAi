package com.quizAi.backend.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@Table(name = "quiz_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Lob
    @Column(nullable = false)
    private String question;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private List<String> options;

    @Column(nullable = false, length = 255)
    private String answer;

    @Lob
    @Column(nullable = false)
    private String explanation;

    @Lob
    @Column(name = "code_snippet")
    private String codeSnippet;
}
