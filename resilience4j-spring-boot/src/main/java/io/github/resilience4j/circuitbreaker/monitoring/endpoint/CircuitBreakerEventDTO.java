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
package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;

@JsonInclude(JsonInclude.Include.NON_NULL)
class CircuitBreakerEventDTO {

    private final String circuitBreakerName;
    private final CircuitBreakerEvent.Type type;
    private final String creationTime;
    private final String errorMessage;
    private final Long durationInMs;
    private final CircuitBreaker.StateTransition stateTransition;

    CircuitBreakerEventDTO(String circuitBreakerName,
                           CircuitBreakerEvent.Type type,
                           String creationTime,
                          String errorMessage,
                           Long durationInMs,
                           CircuitBreaker.StateTransition stateTransition) {
        this.circuitBreakerName = circuitBreakerName;
        this.type = type;
        this.creationTime = creationTime;
        this.errorMessage = errorMessage;
        this.durationInMs = durationInMs;
        this.stateTransition = stateTransition;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public CircuitBreakerEvent.Type getType() {
        return type;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getDurationInMs() {
        return durationInMs;
    }

    public CircuitBreaker.StateTransition getStateTransition() {
        return stateTransition;
    }
}
