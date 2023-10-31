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
import io.github.resilience4j.micrometer.event.TimerOnFailureEvent;
import io.github.resilience4j.micrometer.event.TimerOnStartEvent;
import io.github.resilience4j.micrometer.event.TimerOnSuccessEvent;

/**
 * Timer event DTO factory
 */
public class TimerEventDTOFactory {

    private TimerEventDTOFactory() {
    }

    public static TimerEventDTO createTimerEventDTO(TimerEvent event) {
        switch (event.getEventType()) {
            case START:
                TimerOnStartEvent onStartEvent = (TimerOnStartEvent) event;
                return new TimerEventDTO(
                        onStartEvent.getTimerName(), onStartEvent.getEventType(), onStartEvent.getCreationTime().toString()
                );
            case SUCCESS:
                TimerOnSuccessEvent onSuccessEvent = (TimerOnSuccessEvent) event;
                return new TimerEventDTO(
                        onSuccessEvent.getTimerName(), onSuccessEvent.getEventType(), onSuccessEvent.getCreationTime().toString(), onSuccessEvent.getOperationDuration()
                );
            case FAILURE:
                TimerOnFailureEvent onFailureEvent = (TimerOnFailureEvent) event;
                return new TimerEventDTO(
                        onFailureEvent.getTimerName(), onFailureEvent.getEventType(), onFailureEvent.getCreationTime().toString(), onFailureEvent.getOperationDuration()
                );
            default:
                throw new IllegalArgumentException("Invalid event");
        }
    }
}
