/*
 * Copyright 2018 Julien Hoarau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
        return new CircuitBreakerMetrics(circuitBreakers, prefix);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            final String name = circuitBreaker.getName();
            Gauge.builder(getName(prefix, STATE), circuitBreaker, (cb) -> cb.getState().getOrder())
                    .tag(NAME_TAG, name)
                    .register(registry);

            Gauge.builder(getName(prefix, CALLS), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfFailedCalls())
                    .tag(NAME_TAG, name)
                    .tag(CALL_RESULT_TAG, FAILED)
                    .register(registry);

            Gauge.builder(getName(prefix, CALLS), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfSuccessfulCalls())
                    .tag(NAME_TAG, name)
                    .tag(CALL_RESULT_TAG, SUCCESSFUL)
                    .register(registry);

            Gauge.builder(getName(prefix, CALLS), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfNotPermittedCalls())
                    .tag(NAME_TAG, name)
                    .tag(CALL_RESULT_TAG, NOT_PERMITTED)
                    .register(registry);

            Gauge.builder(getName(prefix, BUFFER), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfBufferedCalls())
                    .tag(NAME_TAG, name)
                    .register(registry);

            Gauge.builder(getName(prefix, CONFIG_BUFFER_MAX), circuitBreaker, (cb) -> cb.getMetrics().getMaxNumberOfBufferedCalls())
                    .tag(NAME_TAG, name)
                    .register(registry);
        }
    }
}
