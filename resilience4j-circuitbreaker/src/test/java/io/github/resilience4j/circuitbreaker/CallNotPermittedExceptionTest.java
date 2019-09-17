package io.github.resilience4j.circuitbreaker;

import org.junit.Test;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.getCallNotPermittedException;
import static org.assertj.core.api.Assertions.assertThat;

public class CallNotPermittedExceptionTest {

    @Test
    public void shouldReturnCorrectMessageWhenStateIsOpen(){
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        circuitBreaker.transitionToOpenState();
        assertThat(getCallNotPermittedException(circuitBreaker).getMessage()).isEqualTo("CircuitBreaker 'testName' is OPEN and does not permit further calls");
    }

    @Test
    public void shouldReturnCorrectMessageWhenStateIsForcedOpen(){
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        circuitBreaker.transitionToForcedOpenState();
        assertThat(getCallNotPermittedException(circuitBreaker).getMessage()).isEqualTo("CircuitBreaker 'testName' is FORCED_OPEN and does not permit further calls");
    }
}
