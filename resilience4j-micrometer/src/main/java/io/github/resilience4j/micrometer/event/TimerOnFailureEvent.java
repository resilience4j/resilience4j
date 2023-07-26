/*
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
 */
package io.github.resilience4j.micrometer.event;

import java.time.Duration;

import static io.github.resilience4j.micrometer.event.TimerEvent.Type.FAILURE;

/**
 * A TimerEvent which informs that the decorated operation threw an exception.
 */
public class TimerOnFailureEvent extends AbstractTimerEvent {

    private final Duration operationDuration;

    public TimerOnFailureEvent(String monitorName, Duration operationDuration) {
        super(monitorName, FAILURE);
        this.operationDuration = operationDuration;
    }

    /**
     * @return The duration of the decorated operation
     */
    public Duration getOperationDuration() {
        return operationDuration;
    }
}
