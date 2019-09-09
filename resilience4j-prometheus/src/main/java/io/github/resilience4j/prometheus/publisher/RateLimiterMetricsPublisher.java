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

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractRateLimiterMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class RateLimiterMetricsPublisher extends AbstractRateLimiterMetrics implements MetricsPublisher<RateLimiter> {

    private final GaugeMetricFamily availablePermissionsFamily;
    private final GaugeMetricFamily waitingThreadsFamily;

    public RateLimiterMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public RateLimiterMetricsPublisher(MetricNames names) {
        super(names);
        availablePermissionsFamily = new GaugeMetricFamily(
                names.getAvailablePermissionsMetricName(),
                "The number of available permissions",
                LabelNames.NAME
        );

        waitingThreadsFamily = new GaugeMetricFamily(
                names.getWaitingThreadsMetricName(),
                "The number of waiting threads",
                LabelNames.NAME
        );
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return asList(availablePermissionsFamily, waitingThreadsFamily);
    }

    @Override
    public void publishMetrics(RateLimiter entry) {
        List<String> nameLabel = singletonList(entry.getName());
        availablePermissionsFamily.addMetric(nameLabel, entry.getMetrics().getAvailablePermissions());
        waitingThreadsFamily.addMetric(nameLabel, entry.getMetrics().getNumberOfWaitingThreads());
    }

    @Override
    public void removeMetrics(RateLimiter entry) {
        // Do nothing
    }

}
