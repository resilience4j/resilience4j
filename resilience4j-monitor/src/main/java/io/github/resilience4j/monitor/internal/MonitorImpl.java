/*
 *
 *  Copyright 2023 Mariusz Kopylec
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.monitor.internal;

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.monitor.Monitor;
import io.github.resilience4j.monitor.MonitorConfig;
import io.github.resilience4j.monitor.event.MonitorEvent;
import io.github.resilience4j.monitor.event.MonitorOnFailureEvent;
import io.github.resilience4j.monitor.event.MonitorOnStartEvent;
import io.github.resilience4j.monitor.event.MonitorOnSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

public class MonitorImpl implements Monitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorImpl.class);

    private final String name;
    private final Map<String, String> tags;
    private final MonitorConfig monitorConfig;
    private final MonitorEventProcessor eventProcessor;
    private final MonitorLogger monitorLogger;

    public MonitorImpl(@NonNull String name, @NonNull MonitorConfig monitorConfig) {
        this(name, monitorConfig, emptyMap());
    }

    public MonitorImpl(@NonNull String name, @NonNull MonitorConfig monitorConfig, @NonNull Map<String, String> tags) {
        this.name = requireNonNull(name, "Name must not be null");
        this.monitorConfig = requireNonNull(monitorConfig, "Monitor config must not be null");
        this.tags = requireNonNull(tags, "Tags must not be null");
        this.eventProcessor = new MonitorEventProcessor();
        monitorLogger = new MonitorLogger(monitorConfig.getLogMode(), monitorConfig.getLogLevel());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public MonitorConfig getMonitorConfig() {
        return monitorConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onStart(Object input) {
        monitorLogger.logStart(name, input);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new MonitorOnStartEvent(name));
        }
    }

    @Override
    public void onFailure(Duration operationExecutionDuration, Object input, String resultName, Throwable throwable) {
        monitorLogger.logFailure(name, input, resultName, throwable);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new MonitorOnFailureEvent(name, operationExecutionDuration, resultName));
        }
    }

    @Override
    public void onSuccess(Duration operationExecutionDuration, Object input, String resultName, Object output) {
        monitorLogger.logSuccess(name, input, resultName, output);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new MonitorOnSuccessEvent(name, operationExecutionDuration, resultName));
        }
    }

    private void publishEvent(MonitorEvent event) {
        try {
            eventProcessor.consumeEvent(event);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }
}

