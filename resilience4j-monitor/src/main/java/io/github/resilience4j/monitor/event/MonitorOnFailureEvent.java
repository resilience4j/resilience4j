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

import java.time.Duration;

import static io.github.resilience4j.monitor.event.MonitorEvent.Type.FAILURE;

/**
 * A MonitorEvent which informs that the operation execution threw an exception.
 */
public class MonitorOnFailureEvent extends AbstractMonitorEvent {

    private final Duration operationExecutionDuration;
    private final String resultName;

    public MonitorOnFailureEvent(String monitorName, Duration operationExecutionDuration, String resultName) {
        super(monitorName, FAILURE);
        this.operationExecutionDuration = operationExecutionDuration;
        this.resultName = resultName;
    }

    /**
     * @return The duration of the operation execution
     */
    public Duration getOperationExecutionDuration() {
        return operationExecutionDuration;
    }

    /**
     * @return The result name of the operation execution
     */
    public String getResultName() {
        return resultName;
    }
}
