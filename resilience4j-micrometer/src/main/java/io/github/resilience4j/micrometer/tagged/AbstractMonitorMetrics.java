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

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.monitor.Monitor;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.resilience4j.micrometer.tagged.TagNames.KIND;
import static io.github.resilience4j.micrometer.tagged.TagNames.NAME;
import static java.util.Objects.requireNonNull;

abstract class AbstractMonitorMetrics extends AbstractMetrics {

    private static final String RESULT = "result";
    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";

    @NonNull
    protected final MonitorMetricNames names;

    protected AbstractMonitorMetrics(@NonNull MonitorMetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, Monitor monitor) {
        List<Tag> customTags = mapToTagsList(monitor.getTags());
        registerMetrics(meterRegistry, monitor, customTags);
    }

    private void registerMetrics(MeterRegistry meterRegistry, Monitor monitor, List<Tag> customTags) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, monitor.getName());
        Set<Meter.Id> idSet = new HashSet<>();
        monitor.getEventPublisher()
                .onSuccess(event -> {
                    Meter.Id id = recordCall(meterRegistry, monitor, customTags, KIND_SUCCESSFUL, event.getOperationExecutionDuration(), event.getResultName());
                    idSet.add(id);
                })
                .onFailure(event -> {
                    Meter.Id id = recordCall(meterRegistry, monitor, customTags, KIND_FAILED, event.getOperationExecutionDuration(), event.getResultName());
                    idSet.add(id);
                });
        meterIdMap.put(monitor.getName(), idSet);
    }

    private Meter.Id recordCall(MeterRegistry meterRegistry, Monitor monitor, List<Tag> customTags, String kindTag, Duration operationExecutionDuration, String resultName) {
        Timer calls = Timer.builder(names.getCallsMetricName())
                .description("Executed calls")
                .tag(NAME, monitor.getName())
                .tags(customTags)
                .tag(KIND, kindTag)
                .tag(RESULT, resultName)
                .register(meterRegistry);
        calls.record(operationExecutionDuration);
        return calls.getId();
    }
}
