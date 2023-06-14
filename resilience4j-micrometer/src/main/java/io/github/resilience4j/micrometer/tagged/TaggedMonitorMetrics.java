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

import io.github.resilience4j.monitor.MonitorRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder for Monitor metrics.
 */
public class TaggedMonitorMetrics extends AbstractMonitorMetrics implements MeterBinder {

    private final MonitorRegistry monitorRegistry;

    private TaggedMonitorMetrics(MonitorMetricNames names, MonitorRegistry monitorRegistry) {
        super(names);
        this.monitorRegistry = requireNonNull(monitorRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of monitors.
     *
     * @param monitorRegistry the source of monitors
     * @return The {@link TaggedMonitorMetrics} instance.
     */
    public static TaggedMonitorMetrics ofMonitorRegistry(MonitorRegistry monitorRegistry) {
        return new TaggedMonitorMetrics(MonitorMetricNames.ofDefaults(), monitorRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of monitors.
     *
     * @param names           custom metric names
     * @param monitorRegistry the source of monitors
     * @return The {@link TaggedMonitorMetrics} instance.
     */
    public static TaggedMonitorMetrics ofMonitorRegistry(MonitorMetricNames names, MonitorRegistry monitorRegistry) {
        return new TaggedMonitorMetrics(names, monitorRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        monitorRegistry.getAllMonitors().forEach(monitor -> {
            addMetrics(registry, monitor);
        });
        monitorRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        monitorRegistry.getEventPublisher().onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        monitorRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
