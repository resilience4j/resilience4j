/*
 * Copyright 2019 authors
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

import io.github.resilience4j.prometheus.AbstractTimeLimiterMetrics;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Collects TimeLimiter exposed events.
 */
public class TimeLimiterMetricsCollector extends AbstractTimeLimiterMetrics {

    private final TimeLimiterRegistry timeLimiterRegistry;

    private TimeLimiterMetricsCollector(MetricNames names,
        TimeLimiterRegistry timeLimiterRegistry) {
        super(names);
        this.timeLimiterRegistry = requireNonNull(timeLimiterRegistry);
        this.timeLimiterRegistry.getAllTimeLimiters()
            .forEach(this::addMetrics);
        this.timeLimiterRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(event.getAddedEntry()));
    }

    /**
     * Creates a new collector with custom metric names and using given {@code supplier} as source
     * of time limiters.
     *
     * @param names               the custom metric names
     * @param timeLimiterRegistry the source of time limiters
     */
    public static TimeLimiterMetricsCollector ofTimeLimiterRegistry(MetricNames names,
        TimeLimiterRegistry timeLimiterRegistry) {
        return new TimeLimiterMetricsCollector(names, timeLimiterRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of time limiters.
     *
     * @param timeLimiterRegistry the source of time limiters
     */
    public static TimeLimiterMetricsCollector ofTimeLimiterRegistry(
        TimeLimiterRegistry timeLimiterRegistry) {
        return new TimeLimiterMetricsCollector(MetricNames.ofDefaults(), timeLimiterRegistry);
    }

    private void addMetrics(TimeLimiter timeLimiter) {
        String name = timeLimiter.getName();
        timeLimiter.getEventPublisher()
            .onSuccess(event -> callsCounter.labels(name, KIND_SUCCESSFUL).inc())
            .onError(event -> callsCounter.labels(name, KIND_FAILED).inc())
            .onTimeout(event -> callsCounter.labels(name, KIND_TIMEOUT).inc());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Collections.list(collectorRegistry.metricFamilySamples());
    }

}
