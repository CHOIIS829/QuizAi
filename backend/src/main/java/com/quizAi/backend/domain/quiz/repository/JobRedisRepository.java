package com.quizAi.backend.domain.quiz.repository;

import com.quizAi.backend.domain.quiz.dto.QuizJobRecord;
import com.quizAi.backend.domain.quiz.dto.QuizResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class JobRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // 데이터 유효 시간 (30분)
    private static final long JOB_TTL = 30;
    private static final String JOB_KEY_PREFIX = "quiz:job:";

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

    public List<QuizJobRecord> findAllJobs() {
        Set<String> keys = redisTemplate.keys(JOB_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(QuizJobRecord.class::cast)
                .toList();
    }

    public void markFailedIfStale(String jobId, Instant staleBefore) {
        update(jobId, record -> {
            if (record.getStatus() == QuizResponseDto.JobStatus.PROCESSING
                    && record.getCreatedAt() != null
                    && record.getCreatedAt().isBefore(staleBefore)) {
                record.setStatus(QuizResponseDto.JobStatus.FAILED);
                record.setMessage("서버 재시작 또는 처리 시간 초과로 작업이 종료되었습니다.");
                record.setResult(null);
                record.setPersistedQuizId(null);
            }
        });
    }

    private String buildKey(String jobId) {
        return JOB_KEY_PREFIX + jobId;
    }
}
