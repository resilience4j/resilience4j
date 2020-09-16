/*
 *
 *  Copyright 2017: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link CircuitBreaker.Metrics} as Dropwizard Metrics Gauges.
 */
public class CircuitBreakerMetrics implements MetricSet {

    private final MetricRegistry metricRegistry;

    private CircuitBreakerMetrics(Iterable<CircuitBreaker> circuitBreakers) {
        this(DEFAULT_PREFIX, circuitBreakers, new MetricRegistry());
    }

    private CircuitBreakerMetrics(String prefix, Iterable<CircuitBreaker> circuitBreakers,
        MetricRegistry metricRegistry) {
        requireNonNull(prefix);
        requireNonNull(circuitBreakers);
        requireNonNull(metricRegistry);
        this.metricRegistry = metricRegistry;
        circuitBreakers.forEach((CircuitBreaker circuitBreaker) -> {
                String name = circuitBreaker.getName();
                //state as an integer
                metricRegistry.register(name(prefix, name, STATE),
                    (Gauge<Integer>) () -> circuitBreaker.getState().getOrder());
                metricRegistry.register(name(prefix, name, SUCCESSFUL),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
                metricRegistry.register(name(prefix, name, FAILED),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfFailedCalls());
                metricRegistry.register(name(prefix, name, NOT_PERMITTED),
                    (Gauge<Long>) () -> circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
                metricRegistry.register(name(prefix, name, BUFFERED),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfBufferedCalls());
                metricRegistry.register(name(prefix, name, FAILURE_RATE),
                    (Gauge<Float>) () -> circuitBreaker.getMetrics().getFailureRate());
                metricRegistry.register(name(prefix, name, SLOW),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfSlowCalls());
                metricRegistry.register(name(prefix, name, SLOW_SUCCESS),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics()
                        .getNumberOfSlowSuccessfulCalls());
                metricRegistry.register(name(prefix, name, SLOW_FAILED),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfSlowFailedCalls());
                metricRegistry.register(name(prefix, name, SLOW_CALL_RATE),
                    (Gauge<Float>) () -> circuitBreaker.getMetrics().getSlowCallRate());
            }
        );
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with specified
     * metrics names prefix and a {@link CircuitBreakerRegistry} as a source.
     *
     * @param prefix                 the prefix of metrics names
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(String prefix,
        CircuitBreakerRegistry circuitBreakerRegistry, MetricRegistry metricRegistry) {
        return new CircuitBreakerMetrics(prefix, circuitBreakerRegistry.getAllCircuitBreakers(),
            metricRegistry);
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with specified
     * metrics names prefix and a {@link CircuitBreakerRegistry} as a source.
     *
     * @param prefix                 the prefix of metrics names
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(String prefix,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(prefix, circuitBreakerRegistry.getAllCircuitBreakers(),
            new MetricRegistry());
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with a {@link
     * CircuitBreakerRegistry} as a source.
     *
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(
        CircuitBreakerRegistry circuitBreakerRegistry, MetricRegistry metricRegistry) {
        return new CircuitBreakerMetrics(DEFAULT_PREFIX,
            circuitBreakerRegistry.getAllCircuitBreakers(), metricRegistry);
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with a {@link
     * CircuitBreakerRegistry} as a source.
     *
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(circuitBreakerRegistry.getAllCircuitBreakers());
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with an {@link
     * Iterable} of circuit breakers as a source.
     *
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerMetrics ofIterable(Iterable<CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetrics(circuitBreakers);
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with an {@link
     * Iterable} of circuit breakers as a source.
     *
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerMetrics ofIterable(String prefix,
        Iterable<CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetrics(prefix, circuitBreakers, new MetricRegistry());
    }


    /**
     * Creates a new instance of CircuitBreakerMetrics {@link CircuitBreakerMetrics} with a circuit
     * breaker as a source.
     *
     * @param circuitBreaker the circuit breaker
     */
    public static CircuitBreakerMetrics ofCircuitBreaker(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerMetrics(List.of(circuitBreaker));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
