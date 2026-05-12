package io.github.resilience4j.circuitbreaker.utils;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.DISABLED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.FORCED_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.METRICS_ONLY;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerUtilTest {

    @Test
    void shouldConsiderAllKnownStatusesUsingIsCallPermitted() {
        assertThat(State.values())
            .describedAs("List of statuses changed." +
                "Please consider updating CircuitBreakerUtil#isCallPermitted to handle" +
                "new status properly.")
            .containsOnly(DISABLED, CLOSED, OPEN, FORCED_OPEN, HALF_OPEN, METRICS_ONLY);
    }
}
