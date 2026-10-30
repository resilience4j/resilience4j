/*
 *
 *  Copyright 2026 Resilience4j contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledSupplierFailureRetryTest {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void tearDown() throws InterruptedException {
        scheduler.shutdownNow();
        assertThat(scheduler.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }

    private Retry retry() {
        return Retry.of("id", RetryConfig.custom().maxAttempts(3)
            .waitDuration(Duration.ofMillis(1)).build());
    }

    @Test
    void shouldCompletePromiseWhenSupplierThrowsDuringErrorRetry() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new IllegalStateException("supplier failed");
        CompletionStage<String> result = retry().executeCompletionStage(scheduler, () -> {
            if (calls.incrementAndGet() == 1) {
                return CompletableFuture.failedFuture(new RuntimeException("retry me"));
            }
            throw failure;
        });

        assertThatThrownBy(() -> result.toCompletableFuture().get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class).hasCause(failure);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldCompletePromiseWhenSupplierThrowsDuringResultRetry() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new IllegalStateException("supplier failed");
        Retry retry = Retry.of("id", RetryConfig.<String>custom().maxAttempts(3)
            .waitDuration(Duration.ofMillis(1)).retryOnResult("retry"::equals).build());
        CompletionStage<String> result = retry.executeCompletionStage(scheduler, () -> {
            if (calls.incrementAndGet() == 1) {
                return CompletableFuture.completedFuture("retry");
            }
            throw failure;
        });

        assertThatThrownBy(() -> result.toCompletableFuture().get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class).hasCause(failure);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldCompletePromiseWhenScheduledSupplierThrowsError() {
        AtomicInteger calls = new AtomicInteger();
        AssertionError failure = new AssertionError("supplier failed");
        CompletionStage<String> result = retry().executeCompletionStage(scheduler, () -> {
            if (calls.incrementAndGet() == 1) {
                return CompletableFuture.failedFuture(new RuntimeException("retry me"));
            }
            throw failure;
        });

        assertThatThrownBy(() -> result.toCompletableFuture().get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class).hasCause(failure);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldContinueRetryingFailuresReportedThroughStages() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CompletionStage<String> result = retry().executeCompletionStage(scheduler, () ->
            calls.incrementAndGet() < 3
                ? CompletableFuture.failedFuture(new RuntimeException("retry me"))
                : CompletableFuture.completedFuture("ok"));

        assertThat(result.toCompletableFuture().get(2, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void shouldPreserveImmediateSupplierFailure() {
        RuntimeException failure = new IllegalStateException("initial call failed");

        assertThatThrownBy(() -> retry().executeCompletionStage(scheduler, () -> {
            throw failure;
        })).isSameAs(failure);
    }
}
