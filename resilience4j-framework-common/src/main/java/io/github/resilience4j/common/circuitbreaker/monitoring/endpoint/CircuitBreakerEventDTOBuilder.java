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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;

class CircuitBreakerEventDTOBuilder {

    private final String circuitBreakerName;
    private final CircuitBreakerEvent.Type type;
    private final String creationTime;
    @Nullable
    private String throwable = null;
    @Nullable
    private Long duration = null;
    @Nullable
    private CircuitBreaker.StateTransition stateTransition = null;

    CircuitBreakerEventDTOBuilder(String circuitBreakerName, CircuitBreakerEvent.Type type,
        String creationTime) {
        this.circuitBreakerName = circuitBreakerName;
        this.type = type;
        this.creationTime = creationTime;
    }

    CircuitBreakerEventDTOBuilder setThrowable(Throwable throwable) {
        this.throwable = throwable.toString();
        return this;
    }

    CircuitBreakerEventDTOBuilder setDuration(Duration duration) {
        this.duration = duration.toMillis();
        return this;
    }

    CircuitBreakerEventDTOBuilder setStateTransition(
        CircuitBreaker.StateTransition stateTransition) {
        this.stateTransition = stateTransition;
        return this;
    }

    CircuitBreakerEventDTO build() {
        return new CircuitBreakerEventDTO(circuitBreakerName, type, creationTime, throwable,
            duration, stateTransition);
    }
}