package io.github.resilience4j.circuitbreaker.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtil.isCallPermitted;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CircuitBreakerUtilIsCallPermittedTest {

    private static final boolean CALL_NOT_PERMITTED = false;
    private static final boolean CALL_PERMITTED = true;
    public State state;
    public boolean expectedPermission;

    public static Collection<Object[]> cases() {
        return asList(new Object[][]{
            {State.DISABLED, CALL_PERMITTED},
            {State.CLOSED, CALL_PERMITTED},
            {State.OPEN, CALL_NOT_PERMITTED},
            {State.FORCED_OPEN, CALL_NOT_PERMITTED},
            {State.HALF_OPEN, CALL_PERMITTED},
        });
    }

    private static CircuitBreaker givenCircuitBreakerAtState(State state) {
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreaker.getState()).thenReturn(state);
        return circuitBreaker;
    }

    @MethodSource("cases")
    @ParameterizedTest(name = "isCallPermitted should be {1} for circuit breaker state {0}")
    public void shouldIndicateCallPermittedForGivenStatus(State state, boolean expectedPermission) {
        initCircuitBreakerUtilIsCallPermittedTest(state, expectedPermission);
        CircuitBreaker circuitBreaker = givenCircuitBreakerAtState(state);

        boolean isPermitted = isCallPermitted(circuitBreaker);

        assertThat(isPermitted).isEqualTo(expectedPermission);
    }

    public void initCircuitBreakerUtilIsCallPermittedTest(State state, boolean expectedPermission) {
        this.state = state;
        this.expectedPermission = expectedPermission;
    }
}
