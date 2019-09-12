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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.prometheus.AbstractBulkheadMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/** Collects bulkhead exposed {@link Metrics}. */
public class BulkheadMetricsCollector extends AbstractBulkheadMetrics {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of bulkheads.
     *
     * @param names    the custom metric names
     * @param bulkheadRegistry the source of bulkheads
     */
    public static BulkheadMetricsCollector ofBulkheadRegistry(BulkheadMetricsCollector.MetricNames names, BulkheadRegistry bulkheadRegistry) {
        return new BulkheadMetricsCollector(names, bulkheadRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     */
    public static BulkheadMetricsCollector ofBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadMetricsCollector(BulkheadMetricsCollector.MetricNames.ofDefaults(), bulkheadRegistry);
    }

    private final BulkheadRegistry bulkheadRegistry;

    private BulkheadMetricsCollector(MetricNames names, BulkheadRegistry bulkheadRegistry) {
        super(names);
        this.bulkheadRegistry = requireNonNull(bulkheadRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily availableCallsFamily = new GaugeMetricFamily(
            names.getAvailableConcurrentCallsMetricName(),
            "The number of available concurrent calls",
            LabelNames.NAME
        );
        GaugeMetricFamily maxAllowedCallsFamily = new GaugeMetricFamily(
            names.getMaxAllowedConcurrentCallsMetricName(),
            "The maximum number of allowed concurrent calls",
            LabelNames.NAME
        );

        for (Bulkhead bulkhead: bulkheadRegistry.getAllBulkheads()) {
            List<String> labelValues = singletonList(bulkhead.getName());
            availableCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getAvailableConcurrentCalls());
            maxAllowedCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        }

        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

}
