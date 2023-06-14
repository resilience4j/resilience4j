/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.monitor.Monitor;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedMonitorMetricsPublisher extends AbstractMonitorMetrics implements MetricsPublisher<Monitor> {

    private final MeterRegistry meterRegistry;

    public TaggedMonitorMetricsPublisher(MeterRegistry meterRegistry) {
        super(MonitorMetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedMonitorMetricsPublisher(MonitorMetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(Monitor entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(Monitor entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
