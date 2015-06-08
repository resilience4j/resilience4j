package io.github.robwin.circuitbreaker;

public final class CircuitBreakerUtils {

    private CircuitBreakerUtils(){}

    static void checkIfCallIsPermitted(CircuitBreaker circuitBreaker) {
        if(!circuitBreaker.isCallPermitted()) {
            throw new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
        }
    }
}
