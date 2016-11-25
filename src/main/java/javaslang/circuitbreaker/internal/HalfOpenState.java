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

final class HalfOpenState extends CircuitBreakerState {

    private CircuitBreakerMetrics circuitBreakerMetrics;
    private final float failureRateThreshold;

    HalfOpenState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.getCircuitBreakerConfig();
        this.circuitBreakerMetrics = new CircuitBreakerMetrics(
                circuitBreakerConfig.getRingBufferSizeInHalfOpenState());
        this.failureRateThreshold = stateMachine.getCircuitBreakerConfig().getFailureRateThreshold();
    }

    /**
     * Returns always true, because the CircuitBreaker is half open.
     *
     * @return always true, because the CircuitBreaker is half open.
     */
    @Override
    boolean isCallPermitted() {
        return true;
    }

    @Override
    void recordFailure(Throwable throwable) {
        // Thread-safe
        checkFailureRate(circuitBreakerMetrics.recordFailure());
    }

    @Override
    void recordSuccess() {
        // Thread-safe
        checkFailureRate(circuitBreakerMetrics.recordSuccess());
    }

    /**
     * Checks if the current failure rate is above or below the threshold.
     * If the failure rate is above the threshold, transition the state machine to OPEN state.
     * If the failure rate is below the threshold, transition the state machine to CLOSED state.
     *
     * @param currentFailureRate the current failure rate
     */
    private void checkFailureRate(float currentFailureRate) {
        if(currentFailureRate != -1){
            if(currentFailureRate >= failureRateThreshold) {
                stateMachine.transitionToOpenState(CircuitBreaker.StateTransition.HALF_OPEN_TO_OPEN, circuitBreakerMetrics);
            }else{
                stateMachine.transitionToClosedState(CircuitBreaker.StateTransition.HALF_OPEN_TO_CLOSED);
            }
        }
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.HALF_OPEN;
    }

    @Override
    CircuitBreaker.Metrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
