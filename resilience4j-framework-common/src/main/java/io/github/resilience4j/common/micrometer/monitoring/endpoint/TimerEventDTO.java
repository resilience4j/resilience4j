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

package io.github.resilience4j.common.micrometer.monitoring.endpoint;

import io.github.resilience4j.micrometer.event.TimerEvent;

import java.time.Duration;

public class TimerEventDTO {

    private String timerName;
    private TimerEvent.Type type;
    private String creationTime;
    private Duration operationDuration;

    public TimerEventDTO(String timerName, TimerEvent.Type type, String creationTime) {
        this(timerName, type, creationTime, null);
    }

    public TimerEventDTO(String timerName, TimerEvent.Type type, String creationTime, Duration operationDuration) {
        this.timerName = timerName;
        this.type = type;
        this.creationTime = creationTime;
        this.operationDuration = operationDuration;
    }

    public String getTimerName() {
        return timerName;
    }

    public void setTimerName(String timerName) {
        this.timerName = timerName;
    }

    public TimerEvent.Type getType() {
        return type;
    }

    public void setType(TimerEvent.Type type) {
        this.type = type;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public Duration getOperationDuration() {
        return operationDuration;
    }

    public void setOperationDuration(Duration operationDuration) {
        this.operationDuration = operationDuration;
    }
}
