/*
 *
 *  Copyright 2017 Oleksandr Goldobin
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
package io.github.resilience4j.prometheus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * An adapter from builtin {@link CircuitBreaker.Metrics} to prometheus
 * {@link io.prometheus.client.CollectorRegistry}.
 *
 * Also exports {@link CircuitBreaker} state as a labeled metric
 *
 * @deprecated use {@link io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector} instead.
 */
@Deprecated
public class CircuitBreakerExports extends Collector {

    private static final String DEFAULT_NAME = "resilience4j_circuitbreaker";
    private static Array<Tuple2<CircuitBreaker.State, String>> STATE_NAME_MAP =
            Array.ofAll(asList(CircuitBreaker.State.values()))
                .map(state -> Tuple.of(state, state.name().toLowerCase()));

    private final String prefix;
    private final Supplier<Iterable<CircuitBreaker>> circuitBreakersSupplier;

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link Supplier} of circuit breakers
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakersSupplier the supplier of circuit breakers
     */
    public static CircuitBreakerExports ofSupplier(String prefix, Supplier<Iterable<CircuitBreaker>> circuitBreakersSupplier) {
        return new CircuitBreakerExports(prefix, circuitBreakersSupplier);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link Supplier} of circuit breakers
     *
     * @param circuitBreakersSupplier the supplier of circuit breakers
     */
    public static CircuitBreakerExports ofSupplier(Supplier<Iterable<CircuitBreaker>> circuitBreakersSupplier) {
        return new CircuitBreakerExports(DEFAULT_NAME, circuitBreakersSupplier);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link CircuitBreakerRegistry} as a source of circuit breakers.

     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerExports ofCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerExports(circuitBreakerRegistry);
    }
    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * a circuit breaker as a source.
     *
     * @param circuitBreaker the circuit breaker
     */
    public static CircuitBreakerExports ofCircuitBreaker(CircuitBreaker circuitBreaker) {
        requireNonNull(circuitBreaker);
        return new CircuitBreakerExports(Array.of(circuitBreaker));
    }


    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link Iterable} of circuit breakers.
     *
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerExports ofIterable(Iterable<CircuitBreaker> circuitBreakers) {
        requireNonNull(circuitBreakers);
        return new CircuitBreakerExports(circuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link CircuitBreakerRegistry} as a source of circuit breakers.
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    public static CircuitBreakerExports ofCircuitBreakerRegistry(String prefix, CircuitBreakerRegistry circuitBreakerRegistry) {
        requireNonNull(prefix);
        requireNonNull(circuitBreakerRegistry);
        return new CircuitBreakerExports(prefix, circuitBreakerRegistry);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link Iterable} of circuit breakers.
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakers the circuit breakers
     */
    public static CircuitBreakerExports ofIterable(String prefix, Iterable<CircuitBreaker> circuitBreakers) {
        requireNonNull(prefix);
        requireNonNull(circuitBreakers);
        return new CircuitBreakerExports(prefix, circuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * a circuit breaker as a source.
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreaker the circuit breaker
     */
    public static CircuitBreakerExports ofCircuitBreaker(String prefix, CircuitBreaker circuitBreaker) {
        requireNonNull(prefix);
        requireNonNull(circuitBreaker);
        return new CircuitBreakerExports(prefix, Array.of(circuitBreaker));
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link CircuitBreakerRegistry} as a source of circuit breakers.

     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    private CircuitBreakerExports(CircuitBreakerRegistry circuitBreakerRegistry) {
        this(circuitBreakerRegistry::getAllCircuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link Iterable} of circuit breakers.
     *
     * @param circuitBreakers the circuit breakers
     */
    private CircuitBreakerExports(Iterable<CircuitBreaker> circuitBreakers) {
        this(() -> circuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with default metrics names prefix and
     * {@link Supplier} of circuit breakers
     *
     * @param circuitBreakersSupplier the supplier of circuit breakers
     */
    private CircuitBreakerExports(Supplier<Iterable<CircuitBreaker>> circuitBreakersSupplier) {
        this(DEFAULT_NAME, circuitBreakersSupplier);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link CircuitBreakerRegistry} as a source of circuit breakers.
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakerRegistry the registry of circuit breakers
     */
    private CircuitBreakerExports(String prefix, CircuitBreakerRegistry circuitBreakerRegistry) {
        this(prefix, circuitBreakerRegistry::getAllCircuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link Iterable} of circuit breakers.
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakers the circuit breakers
     */
    private CircuitBreakerExports(String prefix, Iterable<CircuitBreaker> circuitBreakers) {
        this(prefix, () -> circuitBreakers);
    }

    /**
     * Creates a new instance of {@link CircuitBreakerExports} with specified metrics names prefix and
     * {@link Supplier} of circuit breakers
     *
     * @param prefix the prefix of metrics names
     * @param circuitBreakersSupplier the supplier of circuit breakers
     */
    private CircuitBreakerExports(String prefix, Supplier<Iterable<CircuitBreaker>> circuitBreakersSupplier) {
        this.prefix = prefix;
        this.circuitBreakersSupplier = circuitBreakersSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetricFamilySamples> collect() {

        final GaugeMetricFamily states = new GaugeMetricFamily(
                prefix + "_states",
                "Circuit Breaker States",
                asList("name","state"));

        final GaugeMetricFamily calls = new GaugeMetricFamily(
                prefix + "_calls",
                "Circuit Breaker Call Stats",
                asList("name", "call_result"));

        for (CircuitBreaker circuitBreaker : circuitBreakersSupplier.get()) {

            STATE_NAME_MAP.forEach(e -> {
                final CircuitBreaker.State state = e._1;
                final String name = e._2;
                final double value = state == circuitBreaker.getState() ? 1.0 : 0.0;

                states.addMetric(asList(circuitBreaker.getName(), name), value);
            });

            final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            calls.addMetric(
                    asList(circuitBreaker.getName(), "successful"),
                    metrics.getNumberOfSuccessfulCalls());

            calls.addMetric(
                    asList(circuitBreaker.getName(), "failed"),
                    metrics.getNumberOfFailedCalls());

            calls.addMetric(
                    asList(circuitBreaker.getName(), "not_permitted"),
                    metrics.getNumberOfNotPermittedCalls());

            calls.addMetric(
                    asList(circuitBreaker.getName(), "buffered"),
                    metrics.getNumberOfBufferedCalls());

            calls.addMetric(
                    asList(circuitBreaker.getName(), "buffered_max"),
                    metrics.getMaxNumberOfBufferedCalls());
        }

        return asList(calls, states);
    }
}
