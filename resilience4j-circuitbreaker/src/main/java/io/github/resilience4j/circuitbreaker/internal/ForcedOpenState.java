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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

final class ForcedOpenState extends CircuitBreakerState {

    private final CircuitBreakerMetrics circuitBreakerMetrics;

    ForcedOpenState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.getCircuitBreakerConfig();
        final int size = circuitBreakerConfig.getRingBufferSizeInHalfOpenState();
        this.circuitBreakerMetrics = new CircuitBreakerMetrics(size);
    }

    /**
     * Returns always false, and records the rejected call.
     *
     * @return always false, since the FORCED_OPEN state always denies calls.
     */
    @Override
    boolean obtainPermission() {
        circuitBreakerMetrics.onCallNotPermitted();
        return false;
    }

    @Override
    void tryObtainPermission() {
        if(!obtainPermission()){
            throw new CircuitBreakerOpenException(stateMachine);
        }
    }

    /**
     * Should never be called when obtainPermission returns false.
     */
    @Override
    void onError(Throwable throwable) {
        // noOp
    }

    /**
     * Should never be called when obtainPermission returns false.
     */
    @Override
    void onSuccess() {
        // noOp
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.FORCED_OPEN;
    }

    @Override
    CircuitBreakerMetrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
