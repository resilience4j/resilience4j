package io.github.resilience4j.reactor.circuitbreaker.operator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Helper class to test and assert circuit breakers.
 */
class CircuitBreakerAssertions {
    protected final CircuitBreaker circuitBreaker = CircuitBreaker.of("test",
        CircuitBreakerConfig.custom()
            .waitDurationInOpenState(Duration.of(10, ChronoUnit.SECONDS))
            .ringBufferSizeInClosedState(4)
            .ringBufferSizeInHalfOpenState(4)
            .build());

    protected void assertSingleSuccessfulCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
    }

    protected void assertSingleFailedCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    protected void assertNoRegisteredCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
    }
}
