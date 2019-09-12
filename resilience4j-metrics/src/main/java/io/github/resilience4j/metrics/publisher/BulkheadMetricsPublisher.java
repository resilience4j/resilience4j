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

package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.bulkhead.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class BulkheadMetricsPublisher extends AbstractMetricsPublisher<Bulkhead> {

    private final String prefix;

    public BulkheadMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public BulkheadMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public BulkheadMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(Bulkhead bulkhead) {
        String name = bulkhead.getName();

        //number of available concurrent calls as an integer
        String availableConcurrentCalls = name(prefix, name, AVAILABLE_CONCURRENT_CALLS);
        String maxAllowedConcurrentCalls = name(prefix, name, MAX_ALLOWED_CONCURRENT_CALLS);

        metricRegistry.register(availableConcurrentCalls, (Gauge<Integer>) () -> bulkhead.getMetrics().getAvailableConcurrentCalls());
        metricRegistry.register(maxAllowedConcurrentCalls, (Gauge<Integer>) () -> bulkhead.getMetrics().getMaxAllowedConcurrentCalls());

        List<String> metricNames = Arrays.asList(availableConcurrentCalls, maxAllowedConcurrentCalls);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(Bulkhead bulkhead) {
        removeMetrics(bulkhead.getName());
    }
}
