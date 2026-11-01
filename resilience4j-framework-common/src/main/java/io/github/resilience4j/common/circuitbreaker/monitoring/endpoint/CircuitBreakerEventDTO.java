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
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent;
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
    @Nullable
    private CircuitBreaker.HalfOpenPhase previousHalfOpenPhase;
    @Nullable
    private CircuitBreaker.HalfOpenPhase halfOpenPhase;
    @Nullable
    private CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason halfOpenPhaseTransitionReason;

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

    /**
     * Returns the recovery phase before a phase transition event.
     *
     * @return the previous snapshot, or null for other event types
     */
    @Nullable
    public CircuitBreaker.HalfOpenPhase getPreviousHalfOpenPhase() {
        return previousHalfOpenPhase;
    }

    /**
     * Sets the recovery phase preceding a phase transition event.
     *
     * @param previousHalfOpenPhase the previous snapshot, or null for other event types
     */
    public void setPreviousHalfOpenPhase(CircuitBreaker.HalfOpenPhase previousHalfOpenPhase) {
        this.previousHalfOpenPhase = previousHalfOpenPhase;
    }

    /**
     * Returns the recovery phase entered by a phase transition event.
     *
     * @return the current snapshot, or null for other event types
     */
    @Nullable
    public CircuitBreaker.HalfOpenPhase getHalfOpenPhase() {
        return halfOpenPhase;
    }

    /**
     * Sets the recovery phase entered by a phase transition event.
     *
     * @param halfOpenPhase the current snapshot, or null for other event types
     */
    public void setHalfOpenPhase(CircuitBreaker.HalfOpenPhase halfOpenPhase) {
        this.halfOpenPhase = halfOpenPhase;
    }

    /**
     * Returns why a recovery phase was entered.
     *
     * @return the transition reason, or null for other event types
     */
    @Nullable
    public CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason getHalfOpenPhaseTransitionReason() {
        return halfOpenPhaseTransitionReason;
    }

    /**
     * Sets why a recovery phase was entered.
     *
     * @param reason the transition reason, or null for other event types
     */
    public void setHalfOpenPhaseTransitionReason(
        CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason reason) {
        this.halfOpenPhaseTransitionReason = reason;
    }
}
