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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CircuitBreakerMetricsPublisher extends AbstractCircuitBreakerMetrics implements MetricsPublisher<CircuitBreaker> {

    private final List<CircuitBreaker> circuitBreakers = new ArrayList<>();

    public CircuitBreakerMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public CircuitBreakerMetricsPublisher(MetricNames names) {
        this(names, MetricOptions.ofDefaults());
    }

    public CircuitBreakerMetricsPublisher(MetricNames names, MetricOptions options) {
        super(names, options);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = Collections.list(collectorRegistry.metricFamilySamples());
        samples.addAll(collectGaugeSamples(circuitBreakers));
        return samples;
    }

    @Override
    public void publishMetrics(CircuitBreaker entry) {
        addMetrics(entry);
        circuitBreakers.add(entry);
    }

    @Override
    public void removeMetrics(CircuitBreaker entry) {
        circuitBreakers.remove(entry);
    }
}
