package com.quizAi.backend.domain.quiz.entity;

import com.quizAi.backend.domain.quiz.dto.QuizResultDto;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TopicTagCatalog {
    FRONTEND("frontend", "Frontend", List.of("react", "next", "css", "html", "javascript", "typescript", "ui", "브라우저", "프론트")),
    BACKEND("backend", "Backend", List.of("spring", "api", "server", "backend", "jpa", "controller", "서비스", "백엔드")),
    DATABASE("database", "Database", List.of("sql", "mysql", "redis", "database", "index", "transaction", "query", "db")),
    DEVOPS("devops", "DevOps", List.of("docker", "nginx", "deploy", "cicd", "cloud", "infra", "devops", "배포")),
    CS("cs", "CS", List.of("algorithm", "data structure", "operating system", "network", "thread", "memory", "cpu", "자료구조", "운영체제", "네트워크")),
    AI("ai", "AI", List.of("ai", "gemini", "llm", "machine learning", "prompt", "모델", "인공지능")),
    MOBILE("mobile", "Mobile", List.of("android", "ios", "mobile", "kotlin", "swift", "앱")),
    SECURITY("security", "Security", List.of("security", "jwt", "oauth", "auth", "xss", "csrf", "보안", "인증"));

    private final String slug;
    private final String displayName;
    private final List<String> keywords;

    TopicTagCatalog(String slug, String displayName, List<String> keywords) {
        this.slug = slug;
        this.displayName = displayName;
        this.keywords = keywords;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<String> slugs() {
        return Arrays.stream(values())
                .map(TopicTagCatalog::getSlug)
                .toList();
    }

    public static List<String> inferTags(QuizResultDto quizResultDto) {
        String source = ((quizResultDto.getTitle() == null ? "" : quizResultDto.getTitle()) + " "
                + (quizResultDto.getQuestions() == null ? "" : quizResultDto.getQuestions().stream()
                .map(question -> String.join(" ", question.getOptions() == null ? List.of() : question.getOptions()) + " "
                        + question.getQuestion() + " " + question.getExplanation())
                .collect(Collectors.joining(" "))))
                .toLowerCase(Locale.ROOT);

        List<String> matchedTags = Arrays.stream(values())
                .filter(item -> item.keywords.stream().anyMatch(source::contains))
                .map(TopicTagCatalog::getSlug)
                .limit(3)
                .toList();

        return matchedTags.isEmpty() ? List.of(AI.getSlug()) : matchedTags;
    }
}
