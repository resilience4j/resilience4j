package io.github.resilience4j.circuitbreaker.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

import static io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtil.isCallPermitted;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class CircuitBreakerUtilIsCallPermittedTest {

    private static final boolean CALL_NOT_PERMITTED = false;
    private static final boolean CALL_PERMITTED = true;

    @Parameter
    public State state;

    @Parameter(1)
    public boolean expectedPermission;

    @Test
    public void shouldIndicateCallPermittedForGivenStatus() {
        CircuitBreaker circuitBreaker = givenCircuitBreakerAtState(state);

        boolean isPermitted = isCallPermitted(circuitBreaker);

        assertThat(isPermitted).isEqualTo(expectedPermission);
    }

    @Parameters(name = "isCallPermitted should be {1} for circuit breaker state {0}")
    public static Collection<Object[]> cases() {
        return asList(new Object[][] {
                { State.DISABLED, CALL_PERMITTED },
                { State.CLOSED, CALL_PERMITTED },
                { State.OPEN, CALL_NOT_PERMITTED },
                { State.FORCED_OPEN, CALL_NOT_PERMITTED },
                { State.HALF_OPEN, CALL_PERMITTED },
        });
    }

    private static CircuitBreaker givenCircuitBreakerAtState(State state) {
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreaker.getState()).thenReturn(state);
        return circuitBreaker;
    }
}
