package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.quiz.dto.QuizJobRecord;
import com.quizAi.backend.domain.quiz.dto.QuizResponseDto;
import com.quizAi.backend.domain.quiz.repository.JobRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizJobRecoveryService {

    private final JobRedisRepository jobRedisRepository;

    @Value("${app.jobs.stale-after-minutes}")
    private long staleAfterMinutes;

    @Scheduled(fixedDelayString = "${app.jobs.recovery-interval-ms}")
    public void failStaleProcessingJobs() {
        Instant staleBefore = Instant.now().minus(staleAfterMinutes, ChronoUnit.MINUTES);
        for (QuizJobRecord job : jobRedisRepository.findAllJobs()) {
            if (job.getStatus() == QuizResponseDto.JobStatus.PROCESSING
                    && job.getCreatedAt() != null
                    && job.getCreatedAt().isBefore(staleBefore)) {
                log.warn("오래된 PROCESSING 작업을 FAILED로 전환합니다: {}", job.getJobId());
                jobRedisRepository.markFailedIfStale(job.getJobId(), staleBefore);
            }
        }
    }
}
