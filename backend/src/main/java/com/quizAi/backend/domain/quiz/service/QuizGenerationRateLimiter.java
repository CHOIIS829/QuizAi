package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.global.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class QuizGenerationRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final long limit;
    private final Duration window;

    public QuizGenerationRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.rate-limit.quiz-generate.limit}") long limit,
            @Value("${app.rate-limit.quiz-generate.window-seconds}") long windowSeconds
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.limit = limit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void validate(String clientIp) {
        String key = "rate:quiz-generate:" + clientIp;
        Long requestCount = stringRedisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            stringRedisTemplate.expire(key, window);
        }

        if (requestCount != null && requestCount > limit) {
            throw new RateLimitExceededException();
        }
    }
}
