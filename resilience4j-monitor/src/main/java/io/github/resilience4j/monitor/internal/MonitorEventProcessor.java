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

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.monitor.Monitor;
import io.github.resilience4j.monitor.event.MonitorEvent;
import io.github.resilience4j.monitor.event.MonitorOnFailureEvent;
import io.github.resilience4j.monitor.event.MonitorOnSuccessEvent;

public class MonitorEventProcessor extends EventProcessor<MonitorEvent> implements EventConsumer<MonitorEvent>, Monitor.EventPublisher {

    @Override
    public void consumeEvent(@NonNull MonitorEvent event) {
        super.processEvent(event);
    }

    @Override
    public Monitor.EventPublisher onSuccess(EventConsumer<MonitorOnSuccessEvent> eventConsumer) {
        registerConsumer(MonitorOnSuccessEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public Monitor.EventPublisher onFailure(EventConsumer<MonitorOnFailureEvent> eventConsumer) {
        registerConsumer(MonitorOnFailureEvent.class.getName(), eventConsumer);
        return this;
    }
}
