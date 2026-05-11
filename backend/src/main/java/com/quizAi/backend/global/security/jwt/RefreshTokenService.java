package com.quizAi.backend.global.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.refreshTokenTtl = Duration.ofSeconds(refreshTokenSeconds);
    }

    public String issueToken(Long userId) {
        String refreshToken = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(buildKey(refreshToken), String.valueOf(userId), refreshTokenTtl);
        return refreshToken;
    }

    public Optional<Long> findUserIdByToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return Optional.empty();
        }

        String value = stringRedisTemplate.opsForValue().get(buildKey(refreshToken));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        return Optional.of(Long.parseLong(value));
    }

    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        stringRedisTemplate.delete(buildKey(refreshToken));
    }

    private String buildKey(String refreshToken) {
        return REFRESH_TOKEN_PREFIX + refreshToken;
    }
}
