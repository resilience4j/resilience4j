package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerExceptionHandlingTest {

    @Test
    public void shouldRecordRuntimeExceptionAsFailureAndBusinessExceptionAsSuccess() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordException(ex -> !(ex instanceof BusinessException))
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new BusinessException("test"));

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldRecordIOExceptionAsFailureAndBusinessExceptionAsSuccess() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordExceptions(IOException.class)
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new IOException());

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new BusinessException("test"));

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldRecordBusinessExceptionAsFailure() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordException(ex -> "record".equals(ex.getMessage()))
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new BusinessException("record"));

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new BusinessException("bla"));

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldIgnoreNumberFormatException() {
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .ignoreExceptions(NumberFormatException.class)
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
    }

    private static class BusinessException extends Exception {

        public BusinessException(String message) {
            super(message);
        }
    }
}
