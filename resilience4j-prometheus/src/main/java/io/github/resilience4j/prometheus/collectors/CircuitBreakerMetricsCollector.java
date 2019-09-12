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
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/** Collects circuit breaker exposed {@link Metrics}. */
public class CircuitBreakerMetricsCollector extends AbstractCircuitBreakerMetrics {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of circuit breakers.
     *
     * @param names    the custom metric names
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(MetricNames names, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(names, circuitBreakerRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of circuit breakers.
     *
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(), circuitBreakerRegistry);
    }

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreakerMetricsCollector(MetricNames names, CircuitBreakerRegistry circuitBreakerRegistry) {
        super(names);
        this.circuitBreakerRegistry = requireNonNull(circuitBreakerRegistry);

        for (CircuitBreaker circuitBreaker : this.circuitBreakerRegistry.getAllCircuitBreakers()) {
            addMetrics(circuitBreaker);
        }
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(event.getAddedEntry()));
    }

    private void addMetrics(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_NOT_PERMITTED).observe(0))
                .onIgnoredError(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_IGNORED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onSuccess(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_SUCCESSFUL).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onError(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_FAILED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = Collections.list(collectorRegistry.metricFamilySamples());
        samples.addAll(collectGaugeSamples());
        return samples;
    }

    private List<MetricFamilySamples> collectGaugeSamples() {
        GaugeMetricFamily stateFamily = new GaugeMetricFamily(
                names.getStateMetricName(),
                "The state of the circuit breaker:",
                NAME_AND_STATE
        );
        GaugeMetricFamily bufferedCallsFamily = new GaugeMetricFamily(
                names.getBufferedCallsMetricName(),
                "The number of buffered calls",
                LabelNames.NAME_AND_KIND
        );

        GaugeMetricFamily failureRateFamily = new GaugeMetricFamily(
                names.getFailureRateMetricName(),
                "The failure rate",
                LabelNames.NAME
        );

        GaugeMetricFamily slowCallRateFamily = new GaugeMetricFamily(
                names.getSlowCallRateMetricName(),
                "The slow call rate",
                LabelNames.NAME
        );

        for (CircuitBreaker circuitBreaker : this.circuitBreakerRegistry.getAllCircuitBreakers()) {
            final CircuitBreaker.State[] states = CircuitBreaker.State.values();
            for (CircuitBreaker.State state : states) {
                stateFamily.addMetric(asList(circuitBreaker.getName(), state.name().toLowerCase()),
                        circuitBreaker.getState() == state ? 1 : 0);
            }

            List<String> nameLabel = Collections.singletonList(circuitBreaker.getName());
            Metrics metrics = circuitBreaker.getMetrics();
            bufferedCallsFamily.addMetric(asList(circuitBreaker.getName(), KIND_SUCCESSFUL), metrics.getNumberOfSuccessfulCalls());
            bufferedCallsFamily.addMetric(asList(circuitBreaker.getName(), KIND_FAILED), metrics.getNumberOfFailedCalls());
            failureRateFamily.addMetric(nameLabel, metrics.getFailureRate());
            slowCallRateFamily.addMetric(nameLabel, metrics.getSlowCallRate());
        }
        return asList(stateFamily, bufferedCallsFamily, failureRateFamily, slowCallRateFamily);
    }

}
