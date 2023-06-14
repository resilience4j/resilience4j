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
package io.github.resilience4j.monitor.event;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

public abstract class AbstractMonitorEvent implements MonitorEvent {

    private final String monitorName;
    private final Type eventType;
    private final ZonedDateTime creationTime;

    public AbstractMonitorEvent(String monitorName, Type eventType) {
        this.monitorName = monitorName;
        this.eventType = eventType;
        creationTime = now();
    }

    @Override
    public String getMonitorName() {
        return monitorName;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }
}
