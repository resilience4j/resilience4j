package io.github.robwin.circuitbreaker;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Backend circuitBreaker manager.
 * <p/>
 * Constructs backend monitors according to configuration values.
 */
public class InMemoryCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private final CircuitBreakerConfig defaultCircuitBreakerConfig;

    /**
     * The monitors, indexed by name of the backend.
     */
    private final ConcurrentMap<String, CircuitBreaker> monitors;

    /**
     * The constructor with default circuitBreaker properties.
     */
    public InMemoryCircuitBreakerRegistry() {
        this.defaultCircuitBreakerConfig = new CircuitBreakerConfig();
        this.monitors = new ConcurrentHashMap<>();
    }

    /**
     * The constructor with custom default circuitBreaker properties.
     *
     * @param defaultCircuitBreakerConfig The BackendMonitor service properties.
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
        this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig, "CircuitBreakerConfig must not be null");
        this.monitors = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return monitors.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new DefaultCircuitBreaker(name,
                defaultCircuitBreakerConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig customCircuitBreakerConfig) {
        return monitors.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new DefaultCircuitBreaker(name,
                customCircuitBreakerConfig));
    }

    /**
     * Resets the circuitBreaker states.
     */
    public void resetMonitorStates() {
        monitors.values().forEach(CircuitBreaker::recordSuccess);
    }
}
