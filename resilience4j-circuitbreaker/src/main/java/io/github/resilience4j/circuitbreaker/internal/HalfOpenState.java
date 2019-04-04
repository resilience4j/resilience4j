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

import java.util.concurrent.atomic.AtomicInteger;

final class HalfOpenState extends CircuitBreakerState {

    private CircuitBreakerMetrics circuitBreakerMetrics;
    private final float failureRateThreshold;
    private final AtomicInteger testRequestCounter;

    HalfOpenState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.getCircuitBreakerConfig();
        this.circuitBreakerMetrics = new CircuitBreakerMetrics(
                circuitBreakerConfig.getRingBufferSizeInHalfOpenState());
        this.failureRateThreshold = stateMachine.getCircuitBreakerConfig().getFailureRateThreshold();
        this.testRequestCounter = new AtomicInteger(circuitBreakerConfig.getRingBufferSizeInHalfOpenState());
    }

    /**
     * Checks if test request is allowed.
     *
     * Returns true, if test request counter is not zero.
     * Returns false, if test request counter is zero.
     *
     * @return true, if test request counter is not zero.
     */
    @Override
    boolean isCallPermitted() {
        if(testRequestCounter.get() == 0){
            return false;
        }else{
            testRequestCounter.decrementAndGet();
            return true;
        }
    }

    @Override
    void onError(Throwable throwable) {
        // CircuitBreakerMetrics is thread-safe
        checkFailureRate(circuitBreakerMetrics.onError());
    }

    @Override
    void onSuccess() {
        // CircuitBreakerMetrics is thread-safe
        checkFailureRate(circuitBreakerMetrics.onSuccess());
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
                stateMachine.transitionToOpenState();
            }else{
                stateMachine.transitionToClosedState();
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
    CircuitBreakerMetrics getMetrics() {
        return circuitBreakerMetrics;
    }
}
