package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.control.Either;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerResultHandlingTest {

    @Test
    void shouldRecordSpecificStringResultAsAFailureAndAnyOtherAsSuccess() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordResult(result -> result.equals("failure"))
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "success");

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "failure");

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    void shouldRecordSpecificComplexResultAsAFailureAndAnyOtherAsSuccess() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordResult(result ->
                result instanceof Either && ((Either) result).isLeft() && ((Either) result).getLeft().equals("failure")
            )
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, Either.left("accepted fail"));

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, Either.left("failure"));

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }
}
