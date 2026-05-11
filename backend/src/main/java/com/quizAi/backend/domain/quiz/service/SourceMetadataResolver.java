package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.quiz.entity.SourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Component
public class SourceMetadataResolver {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/|embed/|v/)?([a-zA-Z0-9_-]{11}).*$"
    );

    public SourceMetadata resolve(String sourceUrl) {
        String host = resolveHost(sourceUrl);
        return new SourceMetadata(resolveSourceType(sourceUrl, host), host);
    }

    private SourceType resolveSourceType(String sourceUrl, String host) {
        if (sourceUrl != null && YOUTUBE_PATTERN.matcher(sourceUrl).matches()) {
            return SourceType.YOUTUBE;
        }

        if (host.contains("blog") || host.contains("tistory") || host.contains("velog")) {
            return SourceType.BLOG;
        }

        return SourceType.WEB;
    }

    private String resolveHost(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            return uri.getHost() == null ? "unknown" : uri.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
