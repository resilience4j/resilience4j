package io.github.resilience4j.circuitbreaker;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CircuitBreakerOpenExceptionTest {

    @Test
    public void shouldReturnCorrectMessageWhenStateIsOpen() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        circuitBreaker.transitionToOpenState();
        assertThat(createCallNotPermittedException(circuitBreaker).getMessage())
            .isEqualTo("CircuitBreaker 'testName' is OPEN and does not permit further calls");
    }

    @Test
    public void shouldReturnCorrectMessageWhenStateIsForcedOpen() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        circuitBreaker.transitionToForcedOpenState();
        assertThat(createCallNotPermittedException(circuitBreaker).getMessage()).isEqualTo(
            "CircuitBreaker 'testName' is FORCED_OPEN and does not permit further calls");
    }
}
