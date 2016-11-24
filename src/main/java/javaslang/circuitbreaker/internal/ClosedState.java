/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.internal;

import javaslang.circuitbreaker.CircuitBreaker;
import javaslang.circuitbreaker.CircuitBreakerConfig;

final class ClosedState extends CircuitBreakerState {

    private final CircuitBreakerMetrics circuitBreakerMetrics;
    private final float failureRateThreshold;

    ClosedState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.getCircuitBreakerConfig();
        this.circuitBreakerMetrics = new CircuitBreakerMetrics(
                circuitBreakerConfig.getRingBufferSizeInClosedState());
        this.failureRateThreshold = stateMachine.getCircuitBreakerConfig().getFailureRateThreshold();
    }

    /**
     * Returns always true, because the CircuitBreaker is closed.
     *
     * @return always true, because the CircuitBreaker is closed.
     */
    @Override
    boolean isCallPermitted() {
        return true;
    }

    @Override
    void recordFailure(Throwable throwable) {
        checkFailureRate(circuitBreakerMetrics.recordFailure(throwable));
    }

    @Override
    void recordSuccess() {
        checkFailureRate(circuitBreakerMetrics.recordSuccess());
    }

    /**
     * Checks if the current failure rate is above the threshold.
     * If the failure rate is above the threshold, transitions the state machine to OPEN state.
     *
     * @param currentFailureRate the current failure rate
     */
    private void checkFailureRate(float currentFailureRate) {
        if (currentFailureRate >= failureRateThreshold) {
            // Transition the state machine to OPEN state, because the failure rate is above the threshold
            stateMachine.transitionToOpenState(CircuitBreaker.StateTransition.CLOSED_TO_OPEN, circuitBreakerMetrics);
        }
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.CLOSED;
    }
    /**
     *
     * Get metricsof the CircuitBreaker
     */
    @Override
    CircuitBreaker.Metrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
