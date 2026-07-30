package com.quizAi.backend.global.config;

import com.quizAi.backend.domain.quiz.dto.QuizJobRecord;
import com.quizAi.backend.domain.quiz.dto.QuizResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void redisValueSerializerSupportsInstant() {
        QuizJobRecord job = QuizJobRecord.builder()
                .jobId("job-id")
                .status(QuizResponseDto.JobStatus.PROCESSING)
                .createdAt(Instant.parse("2026-07-29T15:21:33.866Z"))
                .build();

        RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        byte[] serialized = serializer.serialize(job);
        QuizJobRecord deserialized = (QuizJobRecord) serializer.deserialize(serialized);

        assertThat(deserialized.getCreatedAt()).isEqualTo(job.getCreatedAt());
    }
}
