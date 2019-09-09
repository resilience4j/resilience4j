/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.prometheus.publisher;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractCircuitBreakerMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class CircuitBreakerMetricsPublisher extends AbstractCircuitBreakerMetrics implements MetricsPublisher<CircuitBreaker> {

    private final GaugeMetricFamily stateFamily;
    private final GaugeMetricFamily bufferedCallsFamily;
    private final GaugeMetricFamily failureRateFamily;
    private final GaugeMetricFamily slowCallRateFamily;

    public CircuitBreakerMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public CircuitBreakerMetricsPublisher(MetricNames names) {
        super(names);
        stateFamily = new GaugeMetricFamily(names.getStateMetricName(), "The state of the circuit breaker:", NAME_AND_STATE);
        bufferedCallsFamily = new GaugeMetricFamily(names.getBufferedCallsMetricName(), "The number of buffered calls", LabelNames.NAME_AND_KIND);
        failureRateFamily = new GaugeMetricFamily(names.getFailureRateMetricName(), "The failure rate", LabelNames.NAME);
        slowCallRateFamily = new GaugeMetricFamily(names.getSlowCallRateMetricName(), "The slow call rate", LabelNames.NAME);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = Collections.list(collectorRegistry.metricFamilySamples());
        samples.addAll(asList(stateFamily, bufferedCallsFamily, failureRateFamily, slowCallRateFamily));
        return samples;
    }

    @Override
    public void publishMetrics(CircuitBreaker entry) {
        final CircuitBreaker.State[] states = CircuitBreaker.State.values();
        for (CircuitBreaker.State state : states) {
            stateFamily.addMetric(asList(entry.getName(), state.name().toLowerCase()),
                    entry.getState() == state ? 1 : 0);
        }

        List<String> nameLabel = Collections.singletonList(entry.getName());
        CircuitBreaker.Metrics metrics = entry.getMetrics();
        bufferedCallsFamily.addMetric(asList(entry.getName(), KIND_SUCCESSFUL), metrics.getNumberOfSuccessfulCalls());
        bufferedCallsFamily.addMetric(asList(entry.getName(), KIND_FAILED), metrics.getNumberOfFailedCalls());
        failureRateFamily.addMetric(nameLabel, metrics.getFailureRate());
        slowCallRateFamily.addMetric(nameLabel, metrics.getSlowCallRate());

        entry.getEventPublisher()
                .onCallNotPermitted(event -> callsHistogram.labels(entry.getName(), KIND_NOT_PERMITTED).observe(0))
                .onIgnoredError(event -> callsHistogram.labels(entry.getName(), KIND_IGNORED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onSuccess(event -> callsHistogram.labels(entry.getName(), KIND_SUCCESSFUL).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onError(event -> callsHistogram.labels(entry.getName(), KIND_FAILED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND));

    }

    @Override
    public void removeMetrics(CircuitBreaker entry) {
        // Do nothing
    }
}
