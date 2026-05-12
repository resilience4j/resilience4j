package io.github.resilience4j.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IllegalStateTransitionExceptionTest {

    @Test
    void shouldReturnCorrectMessage() {
        IllegalStateTransitionException illegalStateTransitionException = new IllegalStateTransitionException(
            "testName", CircuitBreaker.State.OPEN,
            CircuitBreaker.State.CLOSED);
        assertThat(illegalStateTransitionException.getMessage()).isEqualTo(
            "CircuitBreaker 'testName' tried an illegal state transition from OPEN to CLOSED");
        assertThat(illegalStateTransitionException.getFromState())
            .isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(illegalStateTransitionException.getToState())
            .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
