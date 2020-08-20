/*
 * Copyright 2019 Yevhenii Voievodin, Robert Winkler
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
package io.github.resilience4j.prometheus.collectors;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.prometheus.AbstractCircuitBreakerMetrics;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Collects circuit breaker exposed {@link Metrics}.
 */
public class CircuitBreakerMetricsCollector extends AbstractCircuitBreakerMetrics {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreakerMetricsCollector(MetricNames names, MetricOptions options,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        super(names, options);
        this.circuitBreakerRegistry = requireNonNull(circuitBreakerRegistry);

        for (CircuitBreaker circuitBreaker : this.circuitBreakerRegistry.getAllCircuitBreakers()) {
            addMetrics(circuitBreaker);
        }
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(event.getAddedEntry()));
    }

    /**
     * Creates a new collector with custom metric names and using given {@code supplier} as source
     * of circuit breakers.
     *
     * @param names                  the custom metric names
     * @param options                the custom metric options
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(MetricNames names,
        MetricOptions options, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(names, options, circuitBreakerRegistry);
    }

    /**
     * Creates a new collector with custom metric names and using given {@code supplier} as source
     * of circuit breakers.
     *
     * @param names                  the custom metric names
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(MetricNames names,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(names, MetricOptions.ofDefaults(),
            circuitBreakerRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of circuit breakers.
     *
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(),
            MetricOptions.ofDefaults(), circuitBreakerRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = Collections
            .list(collectorRegistry.metricFamilySamples());
        samples
            .addAll(collectGaugeSamples(new ArrayList<>(circuitBreakerRegistry.getAllCircuitBreakers())));
        return samples;
    }

}
