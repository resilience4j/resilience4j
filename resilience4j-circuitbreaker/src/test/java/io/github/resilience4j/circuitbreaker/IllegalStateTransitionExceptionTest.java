package io.github.resilience4j.circuitbreaker;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IllegalStateTransitionExceptionTest {

    @Test
    public void shouldReturnCorrectMessage() {
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
