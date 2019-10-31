/*
 * Copyright 2019 Andrew From
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

package io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.states;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.metrics.CircuitBreakerMetricsDTO;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircuitBreakerStateDTO {

    private String circuitBreakerName;
    private CircuitBreaker.State currentState;
    private CircuitBreakerMetricsDTO metrics;

    CircuitBreakerStateDTO() {
    }

    public CircuitBreakerStateDTO(String circuitBreakerName, CircuitBreaker.State currentState,
        CircuitBreakerMetricsDTO metrics) {
        this.circuitBreakerName = circuitBreakerName;
        this.currentState = currentState;
        this.metrics = metrics;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public void setCircuitBreakerName(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
    }

    public CircuitBreaker.State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CircuitBreaker.State currentState) {
        this.currentState = currentState;
    }

    public CircuitBreakerMetricsDTO getMetrics() {
        return metrics;
    }

    public void setMetrics(CircuitBreakerMetricsDTO metrics) {
        this.metrics = metrics;
    }
}
