package io.github.resilience4j.circuitbreaker.operator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Helper class to test and assert circuit breakers.
 */
class CircuitBreakerAssertions {
    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

    void assertSingleSuccessfulCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
    }

    void assertSingleFailedCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    void assertNoRegisteredCall() {
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
    }
}
