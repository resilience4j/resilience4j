/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.event.*;

public class CircuitBreakerEventDTOFactory {

    private CircuitBreakerEventDTOFactory() {
    }

    public static CircuitBreakerEventDTO createCircuitBreakerEventDTO(CircuitBreakerEvent event) {
        switch (event.getEventType()) {
            case ERROR:
                CircuitBreakerOnErrorEvent onErrorEvent = (CircuitBreakerOnErrorEvent) event;
                return newCircuitBreakerEventDTOBuilder(onErrorEvent)
                    .setThrowable(onErrorEvent.getThrowable())
                    .setDuration(onErrorEvent.getElapsedDuration())
                    .build();
            case SUCCESS:
                CircuitBreakerOnSuccessEvent onSuccessEvent = (CircuitBreakerOnSuccessEvent) event;
                return newCircuitBreakerEventDTOBuilder(onSuccessEvent)
                    .setDuration(onSuccessEvent.getElapsedDuration())
                    .build();
            case STATE_TRANSITION:
                CircuitBreakerOnStateTransitionEvent onStateTransitionEvent = (CircuitBreakerOnStateTransitionEvent) event;
                return newCircuitBreakerEventDTOBuilder(onStateTransitionEvent)
                    .setStateTransition(onStateTransitionEvent.getStateTransition())
                    .build();
            case RESET:
                CircuitBreakerOnResetEvent onResetEvent = (CircuitBreakerOnResetEvent) event;
                return newCircuitBreakerEventDTOBuilder(onResetEvent)
                    .build();
            case IGNORED_ERROR:
                CircuitBreakerOnIgnoredErrorEvent onIgnoredErrorEvent = (CircuitBreakerOnIgnoredErrorEvent) event;
                return newCircuitBreakerEventDTOBuilder(onIgnoredErrorEvent)
                    .setThrowable(onIgnoredErrorEvent.getThrowable())
                    .setDuration(onIgnoredErrorEvent.getElapsedDuration())
                    .build();
            case NOT_PERMITTED:
                CircuitBreakerOnCallNotPermittedEvent onCallNotPermittedEvent = (CircuitBreakerOnCallNotPermittedEvent) event;
                return newCircuitBreakerEventDTOBuilder(onCallNotPermittedEvent)
                    .build();
            default:
                throw new IllegalArgumentException("Invalid event");
        }
    }

    private static CircuitBreakerEventDTOBuilder newCircuitBreakerEventDTOBuilder(
        CircuitBreakerEvent event) {
        return new CircuitBreakerEventDTOBuilder(event.getCircuitBreakerName(),
            event.getEventType(), event.getCreationTime().toString());
    }
}
