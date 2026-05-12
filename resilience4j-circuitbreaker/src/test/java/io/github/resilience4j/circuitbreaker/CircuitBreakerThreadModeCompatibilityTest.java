package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thread mode compatibility test to verify CircuitBreaker works consistently
 * with both platform and virtual threads.
 *
 * This test validates that CircuitBreaker functionality is identical across
 * both thread modes, ensuring safe operation in either environment.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ExtendWith(ThreadModeExtension.class)
class CircuitBreakerThreadModeCompatibilityTest {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerThreadModeCompatibilityTest.class);

    @TestTemplate
    void shouldWorkWithBothThreadModes(ThreadType threadType) throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-" + threadType, config);

        String result = circuitBreaker.executeSupplier(() -> "Hello from " + Thread.currentThread().getName());

        assertThat(result).contains("Hello from");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);

        LOG.info("{} thread test: State={}, SuccessfulCalls={}",
            threadType, circuitBreaker.getState(), circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @TestTemplate
    void shouldHandleFailuresConsistentlyInBothModes(ThreadType threadType) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-failure", config);

        AtomicInteger failures = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        }

        assertThat(failures.get()).isEqualTo(3);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        LOG.info("{} mode failures: {}, State: {}", threadType, failures.get(), circuitBreaker.getState());
    }

    @TestTemplate
    void shouldHandleCompletableFutureOperationsInBothModes(ThreadType threadType) throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-async", config);
        CompletionStage<String> future = circuitBreaker.executeCompletionStage(() ->
            CompletableFuture.supplyAsync(() -> threadType + " async result"));
        String result = future.toCompletableFuture().get();

        assertThat(result).isEqualTo(threadType + " async result");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        LOG.info("{} async: {}", threadType, result);
    }

    @TestTemplate
    void shouldHandleStateTransitionsConsistentlyInBothModes(ThreadType threadType) throws InterruptedException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(50))
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-state", config);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception ignored) {}
        }

        CircuitBreaker.State openState = circuitBreaker.getState();

        Thread.sleep(60);

        try {
            circuitBreaker.executeSupplier(() -> "success");
        } catch (Exception ignored) {}

        CircuitBreaker.State finalState = circuitBreaker.getState();
        LOG.info("{} state transitions: CLOSED -> {} -> {}", threadType, openState, finalState);

        assertThat(finalState).isIn(CircuitBreaker.State.CLOSED, CircuitBreaker.State.HALF_OPEN);
    }
}
