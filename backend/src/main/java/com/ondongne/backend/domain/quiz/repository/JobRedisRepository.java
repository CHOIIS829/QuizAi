package com.ondongne.backend.domain.quiz.repository;

import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class JobRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // 데이터 유효 시간 (30분)
    private static final long JOB_TTL = 30;

    // 저장 (Create / Update)
    public void save(String jobId, QuizResponseDto data) {
        String key = "quiz:job:" + jobId;

        redisTemplate.opsForValue().set(key, data, Duration.ofMinutes(JOB_TTL));
    }

    // 조회 (Read)
    public QuizResponseDto findById(String jobId) {
        String key = "quiz:job:" + jobId;

        return (QuizResponseDto) redisTemplate.opsForValue().get(key);
    }

    // 업데이트 (Update)
    public void update(String jobId, QuizResponseDto.JobStatus status, String message, QuizResultDto result) {
        String key = "quiz:job:" + jobId;

        QuizResponseDto existingData = (QuizResponseDto) redisTemplate.opsForValue().get(key);
        if (existingData != null) {
            existingData.setStatus(status);
            existingData.setMessage(message);
            existingData.setResult(result);

            redisTemplate.opsForValue().set(key, existingData, Duration.ofMinutes(JOB_TTL));
        }
    }
}
