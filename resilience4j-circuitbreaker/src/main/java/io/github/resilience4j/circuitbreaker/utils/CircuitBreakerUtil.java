package io.github.resilience4j.circuitbreaker.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.*;

public final class CircuitBreakerUtil {

    /**
     * Indicates whether Circuit Breaker allows any calls or not.
     *
     * @param circuitBreaker to test
     * @return call is permitted
     */
    public static boolean isCallPermitted(CircuitBreaker circuitBreaker) {
        State state = circuitBreaker.getState();
        return state == CLOSED || state == HALF_OPEN || state == DISABLED;
    }
}
