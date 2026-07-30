package com.quizAi.backend.domain.quiz.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class QuizJobLifecycleTest {

    @Test
    void shutdownWaitsUntilActiveJobFinishes() {
        QuizJobLifecycle lifecycle = new QuizJobLifecycle();
        AtomicBoolean stopped = new AtomicBoolean(false);

        lifecycle.start();
        assertThat(lifecycle.tryStartJob()).isTrue();

        lifecycle.stop(() -> stopped.set(true));
        assertThat(stopped).isFalse();
        assertThat(lifecycle.tryStartJob()).isFalse();

        lifecycle.finishJob();
        assertThat(stopped).isTrue();
    }
}
