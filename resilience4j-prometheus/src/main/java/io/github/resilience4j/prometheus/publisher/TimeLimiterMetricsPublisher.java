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
import io.github.resilience4j.prometheus.AbstractTimeLimiterMetrics;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.Collections;
import java.util.List;

public class TimeLimiterMetricsPublisher extends AbstractTimeLimiterMetrics implements
    MetricsPublisher<TimeLimiter> {

    public TimeLimiterMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public TimeLimiterMetricsPublisher(MetricNames names) {
        super(names);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Collections.list(collectorRegistry.metricFamilySamples());
    }

    @Override
    public void publishMetrics(TimeLimiter entry) {
        String name = entry.getName();
        entry.getEventPublisher()
            .onSuccess(event -> callsCounter.labels(name, KIND_SUCCESSFUL).inc())
            .onError(event -> callsCounter.labels(name, KIND_FAILED).inc())
            .onTimeout(event -> callsCounter.labels(name, KIND_TIMEOUT).inc());
    }

    @Override
    public void removeMetrics(TimeLimiter entry) {
        // Do nothing
    }
}
