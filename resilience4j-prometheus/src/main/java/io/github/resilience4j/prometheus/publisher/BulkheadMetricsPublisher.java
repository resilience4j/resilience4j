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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractBulkheadMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class BulkheadMetricsPublisher extends AbstractBulkheadMetrics implements MetricsPublisher<Bulkhead> {

    private final GaugeMetricFamily availableCallsFamily;
    private final GaugeMetricFamily maxAllowedCallsFamily;

    public BulkheadMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public BulkheadMetricsPublisher(MetricNames names) {
        super(names);
        availableCallsFamily = new GaugeMetricFamily(
                names.getAvailableConcurrentCallsMetricName(),
                "The number of available concurrent calls",
                LabelNames.NAME
        );

        maxAllowedCallsFamily = new GaugeMetricFamily(
                names.getMaxAllowedConcurrentCallsMetricName(),
                "The maximum number of allowed concurrent calls",
                LabelNames.NAME
        );
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

    @Override
    public void publishMetrics(Bulkhead entry) {
        List<String> labelValues = singletonList(entry.getName());
        availableCallsFamily.addMetric(labelValues, entry.getMetrics().getAvailableConcurrentCalls());
        maxAllowedCallsFamily.addMetric(labelValues, entry.getMetrics().getMaxAllowedConcurrentCalls());
    }

    @Override
    public void removeMetrics(Bulkhead entry) {
        // Do nothing
    }
}
