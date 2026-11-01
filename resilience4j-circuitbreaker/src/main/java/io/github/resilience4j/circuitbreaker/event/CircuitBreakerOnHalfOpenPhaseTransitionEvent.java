/*
 *
 *  Copyright 2026 authors
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
package io.github.resilience4j.circuitbreaker.event;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.HalfOpenPhase;

import static java.util.Objects.requireNonNull;

/**
 * Describes entry into, or movement between, phases of HALF_OPEN recovery.
 * The snapshots retain the phase data at the time of the transition.
 * Recovery exit is represented by the ordinary state transition event.
 */
public final class CircuitBreakerOnHalfOpenPhaseTransitionEvent
    extends AbstractCircuitBreakerEvent {

    private final HalfOpenPhase previousPhase;
    private final HalfOpenPhase phase;
    private final Reason reason;

    /**
     * Creates a phase transition event. Recovery entry uses an inactive previous phase;
     * phase advancement and regression retain their previous and current snapshots.
     *
     * @param circuitBreakerName the name of the circuit breaker
     * @param previousPhase the phase before the transition, or the inactive snapshot on entry
     * @param phase the phase entered by the transition
     * @param reason the reason for entering the phase
     * @throws NullPointerException if a phase snapshot or the reason is null
     */
    public CircuitBreakerOnHalfOpenPhaseTransitionEvent(String circuitBreakerName,
        HalfOpenPhase previousPhase, HalfOpenPhase phase, Reason reason) {
        super(circuitBreakerName);
        this.previousPhase = requireNonNull(previousPhase);
        this.phase = requireNonNull(phase);
        this.reason = requireNonNull(reason);
    }

    /**
     * Returns the phase before the transition.
     *
     * @return the previous snapshot, inactive on recovery entry
     */
    public HalfOpenPhase getPreviousPhase() {
        return previousPhase;
    }

    /**
     * Returns the recovery phase entered by the transition.
     *
     * @return the current phase snapshot
     */
    public HalfOpenPhase getPhase() {
        return phase;
    }

    /**
     * Returns why the recovery phase was entered.
     *
     * @return the transition reason
     */
    public Reason getReason() {
        return reason;
    }

    @Override
    public Type getEventType() {
        return Type.HALF_OPEN_PHASE_TRANSITION;
    }

    @Override
    public String toString() {
        return String.format(
            "%s: CircuitBreaker '%s' changed HALF_OPEN recovery phase from %s to %s (%s)",
            getCreationTime(), getCircuitBreakerName(), previousPhase, phase, reason);
    }

    /** Describes entry into, advancement through, or regression along the recovery ramp. */
    public enum Reason {
        /** Recovery began in its initial phase. */
        ENTERED,
        /** A sufficiently sampled healthy phase passed its minimum duration. */
        ADVANCED,
        /** An unhealthy evaluation selected the preceding phase with a fresh health window. */
        STEPPED_BACK
    }
}
