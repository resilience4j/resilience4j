/*
 * Copyright 2020 Vijay Ram
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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;

/**
 * @author vijayram
 */

/**
 * This class is used to emit available data as SSE so that resilence4j SSE can be bridged
 * to suppose legacy hystrix ecosystem of tools such as hystrix dashboard , turbine and vizceral.
 */

public class CircuitBreakerHystrixStreamEventsDTO {

    private CircuitBreakerEvent circuitBreakerRecentEvent;
    private CircuitBreaker.Metrics metrics;
    private CircuitBreaker.State currentState;
    private float failureRateThreshold;
    private float slowCallRateThreshold;

    public CircuitBreakerHystrixStreamEventsDTO(CircuitBreakerEvent circuitBreakerEvent,
                                                CircuitBreaker.State state,
                                                CircuitBreaker.Metrics metrics,
                                                CircuitBreakerConfig circuitBreakerConfig) {
        this.circuitBreakerRecentEvent = circuitBreakerEvent;
        this.metrics = metrics;
        this.currentState = state;
        this.failureRateThreshold = circuitBreakerConfig.getFailureRateThreshold();
        this.slowCallRateThreshold = circuitBreakerConfig.getSlowCallRateThreshold();
    }

    public CircuitBreakerEvent getCircuitBreakerRecentEvent() {
        return circuitBreakerRecentEvent;
    }

    public void setCircuitBreakerRecentEvent(CircuitBreakerEvent circuitBreakerRecentEvent) {
        this.circuitBreakerRecentEvent = circuitBreakerRecentEvent;
    }

    public CircuitBreaker.Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(CircuitBreaker.Metrics metrics) {
        this.metrics = metrics;
    }

    public CircuitBreaker.State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CircuitBreaker.State currentState) {
        this.currentState = currentState;
    }


    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public void setSlowCallRateThreshold(float slowCallRateThreshold) {
        this.slowCallRateThreshold = slowCallRateThreshold;
    }
}
