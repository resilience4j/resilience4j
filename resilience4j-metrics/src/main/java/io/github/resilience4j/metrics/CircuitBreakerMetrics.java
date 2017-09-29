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
import io.vavr.collection.Array;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link CircuitBreaker.Metrics} as Dropwizard Metrics Gauges.
 */
public class CircuitBreakerMetrics implements MetricSet {

    private static final String DEFAULT_PREFIX = "resilience4j.circuitbreaker";
    public static final String SUCCESSFUL = "successful";
    public static final String FAILED = "failed";
    public static final String NOT_PERMITTED = "not_permitted";
    public static final String BUFFERED = "buffered";
    public static final String BUFFERED_MAX = "buffered_max";
    public static final String IS_OPEN = "is_open";
    public static final String CURRENT_STATE = "current_state";

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private CircuitBreakerMetrics(Iterable<CircuitBreaker> circuitBreakers) {
        this(DEFAULT_PREFIX, circuitBreakers);
    }

    private CircuitBreakerMetrics(String prefix, Iterable<CircuitBreaker> circuitBreakers) {
        requireNonNull(prefix);
        requireNonNull(circuitBreakers);
        circuitBreakers.forEach(circuitBreaker -> {
                String name = circuitBreaker.getName();
                CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                //add a metric that reports the status and a boolean on the open state
                metricRegistry.register(name(prefix, name, IS_OPEN),
                      (Gauge<Boolean>)()-> CircuitBreaker.State.OPEN.equals(circuitBreaker.getState()));
                metricRegistry.register(name(prefix, name, CURRENT_STATE),
                     (Gauge<Integer>) () -> circuitBreaker.getState().ordinal());
                metricRegistry.register(name(prefix, name, SUCCESSFUL),
                    (Gauge<Integer>) metrics::getNumberOfSuccessfulCalls);
                metricRegistry.register(name(prefix, name, FAILED),
                    (Gauge<Integer>) metrics::getNumberOfFailedCalls);
                metricRegistry.register(name(prefix, name, NOT_PERMITTED),
                    (Gauge<Long>) metrics::getNumberOfNotPermittedCalls);
                metricRegistry.register(name(prefix, name, BUFFERED),
                    (Gauge<Integer>) metrics::getNumberOfBufferedCalls);
                metricRegistry.register(name(prefix, name, BUFFERED_MAX),
                    (Gauge<Integer>) metrics::getMaxNumberOfBufferedCalls);
            }
        );
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with specified metrics names prefix and
     * a {@link CircuitBreakerRegistry} as a source.
     *
     * @param prefix                 the prefix of metrics names
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerMetrics ofCircuitBreakerRegistry(String prefix, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(prefix, circuitBreakerRegistry.getAllCircuitBreakers());
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

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with
     * an {@link Iterable} of circuit breakers as a source.
     *
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerMetrics ofIterable(Iterable<CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetrics(circuitBreakers);
    }

    /**
     * Creates a new instance CircuitBreakerMetrics {@link CircuitBreakerMetrics} with
     * an {@link Iterable} of circuit breakers as a source.
     *
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerMetrics ofIterable(String prefix, Iterable<CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetrics(prefix, circuitBreakers);
    }


    /**
     * Creates a new instance of CircuitBreakerMetrics {@link CircuitBreakerMetrics} with a circuit breaker as a source.
     *
     * @param circuitBreaker the circuit breaker
     */
    public static CircuitBreakerMetrics ofCircuitBreaker(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerMetrics(Array.of(circuitBreaker));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
