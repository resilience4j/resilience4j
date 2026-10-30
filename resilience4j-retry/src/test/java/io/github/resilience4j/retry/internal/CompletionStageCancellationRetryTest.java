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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CompletionStageCancellationRetryTest {

    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final Queue<Runnable> scheduled = new ArrayDeque<>();

    @BeforeEach
    void setUp() {
        given(scheduler.schedule(any(Runnable.class), anyLong(), eq(MILLISECONDS)))
            .willAnswer(invocation -> {
                scheduled.add(invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
    }

    @Test
    void shouldNotInvokeScheduledErrorRetryAfterCancellation() {
        assertScheduledRetryStopsAfterCancellation(false);
    }

    @Test
    void shouldNotInvokeScheduledResultRetryAfterCancellation() {
        assertScheduledRetryStopsAfterCancellation(true);
    }

    @Test
    void shouldNotScheduleErrorRetryWhenInFlightStageFailsAfterCancellation() {
        assertInFlightCompletionDoesNotScheduleRetry(false);
    }

    @Test
    void shouldNotScheduleResultRetryWhenInFlightStageCompletesAfterCancellation() {
        assertInFlightCompletionDoesNotScheduleRetry(true);
    }

    @Test
    void shouldStillRetryErrorsWithoutCancellation() {
        assertNormalRetryCompletes(false);
    }

    @Test
    void shouldStillRetryResultsWithoutCancellation() {
        assertNormalRetryCompletes(true);
    }

    private void assertScheduledRetryStopsAfterCancellation(boolean retryOnResult) {
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<String> result = retry(retryOnResult).executeCompletionStage(scheduler,
            () -> {
                calls.incrementAndGet();
                return retryableStage(retryOnResult);
            }).toCompletableFuture();
        assertThat(scheduled).hasSize(1);

        assertThat(result.cancel(false)).isTrue();
        scheduled.remove().run();

        assertThat(calls).hasValue(1);
        assertThat(scheduled).isEmpty();
        assertThat(result).isCancelled();
    }

    private void assertInFlightCompletionDoesNotScheduleRetry(boolean retryOnResult) {
        CompletableFuture<String> inFlight = new CompletableFuture<>();
        CompletableFuture<String> result = retry(retryOnResult)
            .executeCompletionStage(scheduler, () -> inFlight).toCompletableFuture();

        assertThat(result.cancel(false)).isTrue();
        if (retryOnResult) {
            inFlight.complete("retry");
        } else {
            inFlight.completeExceptionally(new IllegalStateException("retry"));
        }

        assertThat(scheduled).isEmpty();
        assertThat(result).isCancelled();
    }

    private void assertNormalRetryCompletes(boolean retryOnResult) {
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<String> result = retry(retryOnResult).executeCompletionStage(scheduler,
            () -> calls.incrementAndGet() == 1 ? retryableStage(retryOnResult)
                : CompletableFuture.completedFuture("success")).toCompletableFuture();

        scheduled.remove().run();

        assertThat(result.join()).isEqualTo("success");
        assertThat(calls).hasValue(2);
        assertThat(scheduled).isEmpty();
    }

    private Retry retry(boolean retryOnResult) {
        RetryConfig config = RetryConfig.<String>custom()
            .maxAttempts(3)
            .retryOnResult(result -> retryOnResult && "retry".equals(result))
            .build();
        return Retry.of("cancellation", config);
    }

    private CompletableFuture<String> retryableStage(boolean retryOnResult) {
        return retryOnResult ? CompletableFuture.completedFuture("retry")
            : CompletableFuture.failedFuture(new IllegalStateException("retry"));
    }
}
