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
package io.github.resilience4j.circuitbreaker.monitoring.health;


import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.util.Optional;

/**
 * A Spring Boot health indicators which adds the state of a CircuitBreaker and it's metrics to the health endpoints
 */
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private static final String FAILURE_RATE = "failureRate";
    private static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
    private static final String BUFFERED_CALLS = "bufferedCalls";
    private static final String FAILED_CALLS = "failedCalls";
    private static final String NOT_PERMITTED = "notPermittedCalls";
    private static final String MAX_BUFFERED_CALLS = "maxBufferedCalls";
    private static final String STATE = "state";
    private CircuitBreaker circuitBreaker;

    public CircuitBreakerHealthIndicator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Health health() {
        return Optional.of(circuitBreaker)
                .map(this::mapBackendMonitorState)
                .orElse(Health.up().build());
    }

    private Health mapBackendMonitorState(CircuitBreaker circuitBreaker) {
        CircuitBreaker.State state = circuitBreaker.getState();
        switch (state) {
            case CLOSED:
                return addDetails(Health.up(), circuitBreaker, state).build();
            case OPEN:
                return addDetails(Health.down(), circuitBreaker, state).build();
            case HALF_OPEN:
                return addDetails(Health.unknown(),circuitBreaker, state).build();
            default:
                return addDetails(Health.unknown(), circuitBreaker, state).build();
        }
    }

    private Health.Builder addDetails(Health.Builder builder, CircuitBreaker circuitBreaker, CircuitBreaker.State state) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        builder.withDetail(FAILURE_RATE, metrics.getFailureRate() + "%")
            .withDetail(FAILURE_RATE_THRESHOLD, config.getFailureRateThreshold() + "%")
            .withDetail(MAX_BUFFERED_CALLS, metrics.getMaxNumberOfBufferedCalls())
            .withDetail(BUFFERED_CALLS, metrics.getNumberOfBufferedCalls())
            .withDetail(FAILED_CALLS, metrics.getNumberOfFailedCalls())
            .withDetail(NOT_PERMITTED, metrics.getNumberOfNotPermittedCalls())
            .withDetail(STATE, state);
        return builder;
    }
}
