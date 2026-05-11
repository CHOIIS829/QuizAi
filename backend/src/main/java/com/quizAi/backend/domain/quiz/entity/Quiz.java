package com.quizAi.backend.domain.quiz.entity;

import com.quizAi.backend.domain.member.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Table(name = "quizzes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "source_host", nullable = false, length = 255)
    private String sourceHost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_via", nullable = false, length = 30)
    private QuizCreatedVia createdVia;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Builder.Default
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "quiz_topic_tags",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_tag_id")
    )
    private Set<TopicTag> topicTags = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addQuestion(QuizQuestion question) {
        questions.add(question);
    }

    public void addTopicTags(List<TopicTag> tags) {
        topicTags.addAll(tags);
    }
}
