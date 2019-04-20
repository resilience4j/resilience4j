/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class OpenState extends CircuitBreakerState {

    private final Instant retryAfterWaitDuration;
    private final CircuitBreakerMetrics circuitBreakerMetrics;
    private final Clock clock;

    OpenState(CircuitBreakerStateMachine stateMachine, CircuitBreakerMetrics circuitBreakerMetrics, SchedulerFactory schedulerFactory) {
        super(stateMachine);
        final Duration waitDurationInOpenState = stateMachine.getCircuitBreakerConfig().getWaitDurationInOpenState();
        this.clock = stateMachine.getClock();
        this.retryAfterWaitDuration = clock.instant().plus(waitDurationInOpenState);
        this.circuitBreakerMetrics = circuitBreakerMetrics;

        if (stateMachine.getCircuitBreakerConfig().isAutomaticTransitionFromOpenToHalfOpenEnabled()) {
            ScheduledExecutorService scheduledExecutorService = schedulerFactory.getScheduler();
            scheduledExecutorService.schedule(stateMachine::transitionToHalfOpenState, waitDurationInOpenState.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Returns false, if the wait duration has not elapsed.
     * Returns true, if the wait duration has elapsed and transitions the state machine to HALF_OPEN state.
     *
     * @return false, if the wait duration has not elapsed. true, if the wait duration has elapsed.
     */
    @Override
    boolean isCallPermitted() {
        // Thread-safe
        if (clock.instant().isAfter(retryAfterWaitDuration)) {
            stateMachine.transitionToHalfOpenState();
            return true;
        }
        circuitBreakerMetrics.onCallNotPermitted();
        return false;
    }

    /**
     * Should never be called when isCallPermitted returns false.
     */
    @Override
    void onError(Throwable throwable) {
        // Could be called when Thread 1 invokes isCallPermitted when the state is CLOSED, but in the meantime another
        // Thread 2 calls onError and the state changes from CLOSED to OPEN before Thread 1 calls onError.
        // But the onError event should still be recorded, even if it happened after the state transition.
        circuitBreakerMetrics.onError();
    }

    /**
     * Should never be called when isCallPermitted returns false.
     */
    @Override
    void onSuccess() {
        // Could be called when Thread 1 invokes isCallPermitted when the state is CLOSED, but in the meantime another
        // Thread 2 calls onError and the state changes from CLOSED to OPEN before Thread 1 calls onSuccess.
        // But the onSuccess event should still be recorded, even if it happened after the state transition.
        circuitBreakerMetrics.onSuccess();
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.OPEN;
    }

    @Override
    CircuitBreakerMetrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
