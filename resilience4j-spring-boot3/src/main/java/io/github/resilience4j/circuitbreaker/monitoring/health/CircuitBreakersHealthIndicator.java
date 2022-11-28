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


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

import java.util.Map;
import java.util.stream.Collectors;

import static io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties.InstanceProperties;

/**
 * A Spring Boot health indicators which adds the state of a CircuitBreaker and it's metrics to the
 * health endpoints
 */
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private static final String FAILURE_RATE = "failureRate";
    private static final String SLOW_CALL_RATE = "slowCallRate";
    private static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
    private static final String SLOW_CALL_RATE_THRESHOLD = "slowCallRateThreshold";
    private static final String BUFFERED_CALLS = "bufferedCalls";
    private static final String FAILED_CALLS = "failedCalls";
    private static final String SLOW_CALLS = "slowCalls";
    private static final String SLOW_FAILED_CALLS = "slowFailedCalls";
    private static final String NOT_PERMITTED = "notPermittedCalls";
    private static final String STATE = "state";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
    private final StatusAggregator statusAggregator;

    public CircuitBreakersHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry,
                                          CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                          StatusAggregator statusAggregator) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.statusAggregator = statusAggregator;
    }

    private static Health.Builder addDetails(Health.Builder builder,
                                             CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        builder.withDetail(FAILURE_RATE, metrics.getFailureRate() + "%")
            .withDetail(FAILURE_RATE_THRESHOLD, config.getFailureRateThreshold() + "%")
            .withDetail(SLOW_CALL_RATE, metrics.getSlowCallRate() + "%")
            .withDetail(SLOW_CALL_RATE_THRESHOLD, config.getSlowCallRateThreshold() + "%")
            .withDetail(BUFFERED_CALLS, metrics.getNumberOfBufferedCalls())
            .withDetail(SLOW_CALLS, metrics.getNumberOfSlowCalls())
            .withDetail(SLOW_FAILED_CALLS, metrics.getNumberOfSlowFailedCalls())
            .withDetail(FAILED_CALLS, metrics.getNumberOfFailedCalls())
            .withDetail(NOT_PERMITTED, metrics.getNumberOfNotPermittedCalls())
            .withDetail(STATE, circuitBreaker.getState());
        return builder;
    }

    private boolean allowHealthIndicatorToFail(CircuitBreaker circuitBreaker) {
        return circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
            .map(InstanceProperties::getAllowHealthIndicatorToFail)
            .orElse(false);
    }

    private Health mapBackendMonitorState(CircuitBreaker circuitBreaker) {
        switch (circuitBreaker.getState()) {
            case CLOSED:
                return addDetails(Health.up(), circuitBreaker).build();
            case OPEN:
                boolean allowHealthIndicatorToFail = allowHealthIndicatorToFail(circuitBreaker);

                return addDetails(allowHealthIndicatorToFail ? Health.down() : Health.status("CIRCUIT_OPEN"), circuitBreaker).build();
            case HALF_OPEN:
                return addDetails(Health.status("CIRCUIT_HALF_OPEN"), circuitBreaker).build();
            default:
                return addDetails(Health.unknown(), circuitBreaker).build();
        }
    }

    private boolean isRegisterHealthIndicator(CircuitBreaker circuitBreaker) {
        return circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
            .map(InstanceProperties::getRegisterHealthIndicator)
            .orElse(false);
    }

    @Override
    public Health health() {
        Map<String, Health> healths = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .filter(this::isRegisterHealthIndicator)
            .collect(Collectors.toMap(CircuitBreaker::getName,
                this::mapBackendMonitorState));

        Status status = this.statusAggregator.getAggregateStatus(healths.values().stream().map(Health::getStatus).collect(Collectors.toSet()));
        return Health.status(status).withDetails(healths).build();
    }
}
