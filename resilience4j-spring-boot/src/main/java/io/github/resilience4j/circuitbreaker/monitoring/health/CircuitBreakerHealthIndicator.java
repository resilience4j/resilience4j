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

import java.util.Optional;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;

public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private static final String FAILURE_RATE = "failureRate";
    private static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
    private static final String BUFFERED_CALLS = "bufferedCalls";
    private static final String FAILED_CALLS = "failedCalls";
    private static final String NOT_PERMITTED = "notPermittedCalls";
    private static final String MAX_BUFFERED_CALLS = "maxBufferedCalls";
    private CircuitBreaker circuitBreaker;
    private static final int DEFAULT_BUFFER_SIZE = 100;

    public CircuitBreakerHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry,
                                         EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
                                         CircuitBreakerProperties circuitBreakerProperties,
                                         String backendName) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(backendName, () -> circuitBreakerProperties.createCircuitBreakerConfig(backendName));
        CircuitBreakerProperties.BackendProperties backendProperties = circuitBreakerProperties.getBackends().get(backendName);
        int bufferSize = backendProperties != null ? backendProperties.getEventConsumerBufferSize() : DEFAULT_BUFFER_SIZE;
        circuitBreaker.getEventStream()
                .subscribe(eventConsumerRegistry.createEventConsumer(backendName, bufferSize));
    }

    @Override
    public Health health() {
        return Optional.of(circuitBreaker)
                .map(this::mapBackendMonitorState)
                .orElse(Health.up().build());
    }

    private Health mapBackendMonitorState(CircuitBreaker circuitBreaker) {
        switch (circuitBreaker.getState()) {
            case CLOSED:
                return addDetails(Health.up(), circuitBreaker).build();
            case OPEN:
                return addDetails(Health.down(), circuitBreaker).build();
            case HALF_OPEN:
                return addDetails(Health.unknown(),circuitBreaker).build();
            default:
                return addDetails(Health.unknown(), circuitBreaker).build();
        }
    }

    private Health.Builder addDetails(Health.Builder builder, CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        builder.withDetail(FAILURE_RATE, metrics.getFailureRate() + "%")
            .withDetail(FAILURE_RATE_THRESHOLD, config.getFailureRateThreshold() + "%")
            .withDetail(MAX_BUFFERED_CALLS, metrics.getMaxNumberOfBufferedCalls())
            .withDetail(BUFFERED_CALLS, metrics.getNumberOfBufferedCalls())
            .withDetail(FAILED_CALLS, metrics.getNumberOfFailedCalls())
            .withDetail(NOT_PERMITTED, metrics.getNumberOfNotPermittedCalls());
        return builder;
    }
}
