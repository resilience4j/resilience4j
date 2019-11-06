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

public class CircuitBreakerEventDTO {

    @Nullable
    private String circuitBreakerName;
    @Nullable
    private CircuitBreakerEvent.Type type;
    @Nullable
    private String creationTime;
    @Nullable
    private String errorMessage;
    @Nullable
    private Long durationInMs;
    @Nullable
    private CircuitBreaker.StateTransition stateTransition;

    CircuitBreakerEventDTO() {
    }

    CircuitBreakerEventDTO(String circuitBreakerName,
        CircuitBreakerEvent.Type type,
        String creationTime,
        @Nullable String errorMessage,
        @Nullable Long durationInMs,
        @Nullable CircuitBreaker.StateTransition stateTransition) {
        this.circuitBreakerName = circuitBreakerName;
        this.type = type;
        this.creationTime = creationTime;
        this.errorMessage = errorMessage;
        this.durationInMs = durationInMs;
        this.stateTransition = stateTransition;
    }

    @Nullable
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public void setCircuitBreakerName(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
    }

    @Nullable
    public CircuitBreakerEvent.Type getType() {
        return type;
    }

    public void setType(CircuitBreakerEvent.Type type) {
        this.type = type;
    }

    @Nullable
    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Nullable
    public Long getDurationInMs() {
        return durationInMs;
    }

    public void setDurationInMs(Long durationInMs) {
        this.durationInMs = durationInMs;
    }

    @Nullable
    public CircuitBreaker.StateTransition getStateTransition() {
        return stateTransition;
    }

    public void setStateTransition(CircuitBreaker.StateTransition stateTransition) {
        this.stateTransition = stateTransition;
    }
}
