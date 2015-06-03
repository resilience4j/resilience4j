package io.github.robwin.circuitbreaker;


/**
 * Backend circuitBreaker manager.
 * <p/>
 * Manages backend circuitBreaker objects for the respective backends.
 */
public interface CircuitBreakerRegistry {

    /**
     * Returns the managed {@link CircuitBreaker} or creates a new one with the default configuration.
     *
     * @param name the name of the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    public CircuitBreaker circuitBreaker(String name);

    /**
     * Returns the managed {@link CircuitBreaker} or creates a new one with a custom configuration.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig  the CircuitBreaker configuration
     * @return The {@link CircuitBreaker}
     */
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig circuitBreakerConfig);

    static CircuitBreakerRegistry of(CircuitBreakerConfig defaultCircuitBreakerConfig){
        return new InMemoryCircuitBreakerRegistry(defaultCircuitBreakerConfig);
    }
}
