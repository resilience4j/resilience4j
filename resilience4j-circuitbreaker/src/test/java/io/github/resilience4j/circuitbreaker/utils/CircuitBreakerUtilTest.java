package io.github.resilience4j.circuitbreaker.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.junit.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerUtilTest {

    @Test
    public void shouldConsiderAllKnownStatusesUsingIsCallPermitted() {
        assertThat(State.values())
                .describedAs("List of statuses changed." +
                        "Please consider updating CircuitBreakerUtil#isCallPermitted to handle" +
                        "new status properly.")
                .containsOnly(DISABLED, CLOSED, OPEN, FORCED_OPEN, HALF_OPEN);
    }
}
