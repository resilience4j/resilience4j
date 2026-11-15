package io.github.resilience4j.retry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class RetryTotalCallsTest {

    @ParameterizedTest
    @CsvSource({"SYNC, 1", "SYNC, 2", "SYNC, 3", "ASYNC, 1", "ASYNC, 2", "ASYNC, 3"})
    void shouldCountEachCallOnceWhenResultsAreExhausted(Mode mode, int maxAttempts)
        throws Exception {
        Retry retry = resultRetry(maxAttempts);
        AtomicInteger invocations = new AtomicInteger();

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            String result = mode.execute(retry, scheduler, () -> {
                invocations.incrementAndGet();
                return "retry";
            });

            assertThat(result).isEqualTo("retry");
        }

        assertThat(invocations).hasValue(maxAttempts);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(maxAttempts);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"SYNC, 1", "SYNC, 2", "SYNC, 3", "ASYNC, 1", "ASYNC, 2", "ASYNC, 3"})
    void shouldCountSuccessfulFinalResult(Mode mode, int maxAttempts) throws Exception {
        Retry retry = resultRetry(maxAttempts);
        AtomicInteger invocations = new AtomicInteger();

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            String result = mode.execute(retry, scheduler,
                () -> invocations.incrementAndGet() == maxAttempts ? "success" : "retry");

            assertThat(result).isEqualTo("success");
        }

        assertThat(invocations).hasValue(maxAttempts);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(maxAttempts);
    }

    @ParameterizedTest
    @CsvSource({"SYNC, 1", "SYNC, 2", "SYNC, 3", "ASYNC, 1", "ASYNC, 2", "ASYNC, 3"})
    void shouldCountExceptionAttempts(Mode mode, int maxAttempts) {
        Retry retry = resultRetry(maxAttempts);
        AtomicInteger invocations = new AtomicInteger();
        IOException failure = new IOException("service unavailable");

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            Throwable thrown = catchThrowable(() -> mode.execute(retry, scheduler, () -> {
                invocations.incrementAndGet();
                throw failure;
            }));

            assertThat(mode == Mode.ASYNC ? thrown.getCause() : thrown).isSameAs(failure);
        }

        assertThat(invocations).hasValue(maxAttempts);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(maxAttempts);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void shouldCountAsyncCallsWhenExhaustedResultsThrow(int maxAttempts) {
        Retry retry = Retry.of("test", RetryConfig.<String>custom().maxAttempts(maxAttempts)
            .waitDuration(Duration.ZERO).retryOnResult(result -> true)
            .failAfterMaxAttempts(true).build());
        AtomicInteger invocations = new AtomicInteger();

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            assertThatThrownBy(() -> Mode.ASYNC.execute(retry, scheduler, () -> {
                invocations.incrementAndGet();
                return "retry";
            })).hasCauseInstanceOf(MaxRetriesExceededException.class);
        }

        assertThat(invocations).hasValue(maxAttempts);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(maxAttempts);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void shouldAccumulateCallsAcrossExecutions(Mode mode) throws Exception {
        Retry retry = resultRetry(3);
        AtomicInteger invocations = new AtomicInteger();

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            for (int execution = 0; execution < 3; execution++) {
                mode.execute(retry, scheduler, () -> {
                    invocations.incrementAndGet();
                    return "retry";
                });
            }
        }

        assertThat(invocations).hasValue(9);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(9);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void shouldCountMixedExceptionAndResultRetries(Mode mode) throws Exception {
        Retry retry = resultRetry(3);
        AtomicInteger invocations = new AtomicInteger();

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            mode.execute(retry, scheduler, () -> {
                if (invocations.incrementAndGet() == 1) {
                    throw new IOException("service unavailable");
                }
                return "retry";
            });
        }

        assertThat(invocations).hasValue(3);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(3);
    }

    private Retry resultRetry(int maxAttempts) {
        return Retry.of("test", RetryConfig.<String>custom().maxAttempts(maxAttempts)
            .waitDuration(Duration.ZERO).retryOnResult("retry"::equals)
            .failAfterMaxAttempts(false).build());
    }

    enum Mode {
        SYNC, ASYNC;

        <T> T execute(Retry retry, ScheduledExecutorService scheduler, Callable<T> callable)
            throws Exception {
            if (this == SYNC) {
                return retry.executeCallable(callable);
            }
            return retry.executeCompletionStage(scheduler, () -> {
                try {
                    return CompletableFuture.completedFuture(callable.call());
                } catch (Exception failure) {
                    return CompletableFuture.<T>failedFuture(failure);
                }
            }).toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }
}
