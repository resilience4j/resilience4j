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

import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.prometheus.AbstractThreadPoolBulkheadMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/** Collects bulkhead exposed {@link Metrics}. */
public class ThreadPoolBulkheadMetricsCollector extends AbstractThreadPoolBulkheadMetrics {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of bulkheads.
     *
     * @param names    the custom metric names
     * @param bulkheadRegistry the source of bulkheads
     */
    public static ThreadPoolBulkheadMetricsCollector ofBulkheadRegistry(ThreadPoolBulkheadMetricsCollector.MetricNames names, ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetricsCollector(names, bulkheadRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     */
    public static ThreadPoolBulkheadMetricsCollector ofBulkheadRegistry(ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetricsCollector(ThreadPoolBulkheadMetricsCollector.MetricNames.ofDefaults(), bulkheadRegistry);
    }

    private final ThreadPoolBulkheadRegistry bulkheadRegistry;

    private ThreadPoolBulkheadMetricsCollector(MetricNames names, ThreadPoolBulkheadRegistry bulkheadRegistry) {
        super(names);
        this.bulkheadRegistry = Objects.requireNonNull(bulkheadRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily availableCallsFamily = new GaugeMetricFamily(
            names.getCurrentThreadPoolSizeName(),
            "The number of currently used bulkhead threads",
            LabelNames.NAME
        );
        GaugeMetricFamily maxAllowedCallsFamily = new GaugeMetricFamily(
            names.getAvailableQueueCapacityName(),
            "The number of available bulkhead queue slots",
            LabelNames.NAME
        );

        for (ThreadPoolBulkhead bulkhead: bulkheadRegistry.getAllBulkheads()) {
            List<String> labelValues = singletonList(bulkhead.getName());
            availableCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getThreadPoolSize());
            maxAllowedCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getRemainingQueueCapacity());
        }

        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

}
