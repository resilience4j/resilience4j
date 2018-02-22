package io.github.resilience4j.micrometer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.circuitbreaker.utils.MetricNames.*;
import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static java.util.Objects.requireNonNull;

public class CircuitBreakerMetrics implements MeterBinder {

    private final Iterable<CircuitBreaker> circuitBreakers;
    private final String prefix;

    private CircuitBreakerMetrics(Iterable<CircuitBreaker> circuitBreakers) {
        this(circuitBreakers, DEFAULT_PREFIX);
    }

    private CircuitBreakerMetrics(Iterable<CircuitBreaker> circuitBreakers, String prefix) {
        this.circuitBreakers = requireNonNull(circuitBreakers);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with
     * a {@link CircuitBreakerRegistry} as a source.
     *
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(circuitBreakerRegistry.getAllCircuitBreakers());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            final String name = circuitBreaker.getName();
            Gauge.builder(getName(prefix, name, STATE), circuitBreaker, (cb) -> cb.getState().getOrder())
                    .register(registry);
            Gauge.builder(getName(prefix, name, BUFFERED_MAX), circuitBreaker, (cb) -> cb.getMetrics().getMaxNumberOfBufferedCalls())
                    .register(registry);
            Gauge.builder(getName(prefix, name, BUFFERED), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfBufferedCalls())
                    .register(registry);
            Gauge.builder(getName(prefix, name, FAILED), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfFailedCalls())
                    .register(registry);
            Gauge.builder(getName(prefix, name, NOT_PERMITTED), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfNotPermittedCalls())
                    .register(registry);
            Gauge.builder(getName(prefix, name, SUCCESSFUL), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfSuccessfulCalls())
                    .register(registry);
        }
    }
}
