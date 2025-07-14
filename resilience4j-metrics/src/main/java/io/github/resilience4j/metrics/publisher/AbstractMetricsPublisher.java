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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.core.metrics.MetricsPublisher;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

abstract class AbstractMetricsPublisher<E> implements MetricSet, MetricsPublisher<E> {

    protected final MetricRegistry metricRegistry;
    // Using ConcurrentHashMap for virtual thread optimization - simple metrics name tracking
    protected final ConcurrentMap<String, Set<String>> metricsNameMap = new ConcurrentHashMap<>();

    protected AbstractMetricsPublisher(MetricRegistry metricRegistry) {
        this.metricRegistry = requireNonNull(metricRegistry);
    }

    protected void removeMetrics(String name) {
        Set<String> nameSet = metricsNameMap.get(name);
        if (nameSet != null) {
            nameSet.forEach(metricRegistry::remove);
        }
        metricsNameMap.remove(name);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

}
