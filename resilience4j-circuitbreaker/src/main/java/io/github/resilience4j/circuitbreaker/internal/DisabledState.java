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

final class DisabledState extends CircuitBreakerState {

    private final CircuitBreakerMetrics circuitBreakerMetrics;

    DisabledState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.getCircuitBreakerConfig();
        final int size = circuitBreakerConfig.getRingBufferSizeInClosedState();
        this.circuitBreakerMetrics = new CircuitBreakerMetrics(size);
    }

    /**
     * Returns always true, because the CircuitBreaker is disabled.
     *
     * @return always true, because the CircuitBreaker is disabled.
     */
    @Override
    boolean obtainPermission() {
        return true;
    }

    /**
     * Does not throw an exception, because the CircuitBreaker is disabled.
     */
    @Override
    void tryObtainPermission() {
        // noOp
    }


    @Override
    void onError(Throwable throwable) {
        // noOp
    }

    @Override
    void onSuccess() {
        // noOp
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.DISABLED;
    }
    /**
     *
     * Get metricsof the CircuitBreaker
     */
    @Override
    CircuitBreakerMetrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
