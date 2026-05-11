package com.quizAi.backend.domain.quiz.repository;

import com.quizAi.backend.domain.quiz.dto.QuizJobRecord;
import com.quizAi.backend.domain.quiz.dto.QuizResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class JobRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // 데이터 유효 시간 (30분)
    private static final long JOB_TTL = 30;

    // 저장 (Create / Update)
    public void save(QuizJobRecord data) {
        redisTemplate.opsForValue().set(buildKey(data.getJobId()), data, Duration.ofMinutes(JOB_TTL));
    }

    // 조회 (Read)
    public QuizJobRecord findById(String jobId) {
        return (QuizJobRecord) redisTemplate.opsForValue().get(buildKey(jobId));
    }

    public QuizResponseDto findResponseById(String jobId) {
        QuizJobRecord data = findById(jobId);
        return data == null ? null : data.toResponseDto();
    }

    public void update(String jobId, Consumer<QuizJobRecord> updater) {
        String key = buildKey(jobId);
        QuizJobRecord existingData = (QuizJobRecord) redisTemplate.opsForValue().get(key);
        if (existingData != null) {
            updater.accept(existingData);
            redisTemplate.opsForValue().set(key, existingData, Duration.ofMinutes(JOB_TTL));
        }
    }

    private String buildKey(String jobId) {
        return "quiz:job:" + jobId;
    }
}
