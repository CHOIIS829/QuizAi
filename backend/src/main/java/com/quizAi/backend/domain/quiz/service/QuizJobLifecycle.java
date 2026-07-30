package com.quizAi.backend.domain.quiz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class QuizJobLifecycle implements SmartLifecycle {

    private final AtomicBoolean acceptingJobs = new AtomicBoolean(false);
    private final AtomicInteger activeJobs = new AtomicInteger();
    private final Object monitor = new Object();
    private Runnable shutdownCallback;

    public boolean tryStartJob() {
        synchronized (monitor) {
            if (!acceptingJobs.get()) {
                return false;
            }
            activeJobs.incrementAndGet();
            return true;
        }
    }

    public void finishJob() {
        Runnable callback = null;
        synchronized (monitor) {
            int remaining = activeJobs.updateAndGet(value -> Math.max(0, value - 1));
            if (!acceptingJobs.get() && remaining == 0 && shutdownCallback != null) {
                callback = shutdownCallback;
                shutdownCallback = null;
            }
        }
        if (callback != null) {
            log.info("모든 비동기 퀴즈 작업이 종료되어 애플리케이션 종료를 계속합니다.");
            callback.run();
        }
    }

    @Override
    public void start() {
        acceptingJobs.set(true);
    }

    @Override
    public void stop() {
        stop(() -> { });
    }

    @Override
    public void stop(Runnable callback) {
        synchronized (monitor) {
            acceptingJobs.set(false);
            if (activeJobs.get() == 0) {
                callback.run();
                return;
            }
            log.info("진행 중인 퀴즈 작업 {}개가 끝날 때까지 종료를 대기합니다.", activeJobs.get());
            shutdownCallback = callback;
        }
    }

    @Override
    public boolean isRunning() {
        return acceptingJobs.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
