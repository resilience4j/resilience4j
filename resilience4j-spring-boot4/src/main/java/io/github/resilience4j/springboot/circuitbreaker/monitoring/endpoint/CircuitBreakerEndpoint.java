/*
 * Copyright 2025 Robert Winkler, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerDetails;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerUpdateStateResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.UpdateState;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Endpoint(id = "circuitbreakers")
public class CircuitBreakerEndpoint {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEndpoint(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @ReadOperation
    public CircuitBreakerEndpointResponse getAllCircuitBreakers() {
        Map<String, CircuitBreakerDetails> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .sorted(Comparator.comparing(CircuitBreaker::getName))
            .collect(Collectors.toMap(CircuitBreaker::getName, this::createCircuitBreakerDetails, (v1,v2) -> v1, LinkedHashMap::new));
        return new CircuitBreakerEndpointResponse(circuitBreakers);
    }

    @WriteOperation
    public CircuitBreakerUpdateStateResponse updateCircuitBreakerState(@Selector String name, UpdateState updateState) {
        final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        final String message = "%s state has been changed successfully";
        switch (updateState) {
            case CLOSE:
                circuitBreaker.transitionToClosedState();
                return createCircuitBreakerUpdateStateResponse(name, circuitBreaker.getState().toString(), String.format(message, name));
            case FORCE_OPEN:
                circuitBreaker.transitionToForcedOpenState();
                return createCircuitBreakerUpdateStateResponse(name, circuitBreaker.getState().toString(), String.format(message, name));
            case DISABLE:
                circuitBreaker.transitionToDisabledState();
                return createCircuitBreakerUpdateStateResponse(name, circuitBreaker.getState().toString(), String.format(message, name));
            default:
                return createCircuitBreakerUpdateStateResponse(name, circuitBreaker.getState().toString(), "State change value is not supported please use only " + Arrays.toString(UpdateState.values()));
        }

    }

    private CircuitBreakerUpdateStateResponse createCircuitBreakerUpdateStateResponse(String circuitBreakerName, String newState, String message) {
        CircuitBreakerUpdateStateResponse circuitBreakerUpdateStateResponse = new CircuitBreakerUpdateStateResponse();
        circuitBreakerUpdateStateResponse.setCircuitBreakerName(circuitBreakerName);
        circuitBreakerUpdateStateResponse.setCurrentState(newState);
        circuitBreakerUpdateStateResponse.setMessage(message);

        return circuitBreakerUpdateStateResponse;
    }

    private CircuitBreakerDetails createCircuitBreakerDetails(CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerDetails circuitBreakerDetails = new CircuitBreakerDetails();
        circuitBreakerDetails.setFailureRate(metrics.getFailureRate() + "%");
        circuitBreakerDetails.setFailureRateThreshold(config.getFailureRateThreshold() + "%");
        circuitBreakerDetails.setSlowCallRate(metrics.getSlowCallRate() + "%");
        circuitBreakerDetails.setSlowCallRateThreshold(config.getSlowCallRateThreshold() + "%");
        circuitBreakerDetails.setBufferedCalls(metrics.getNumberOfBufferedCalls());
        circuitBreakerDetails.setSlowCalls(metrics.getNumberOfSlowCalls());
        circuitBreakerDetails.setSlowFailedCalls(metrics.getNumberOfSlowFailedCalls());
        circuitBreakerDetails.setFailedCalls(metrics.getNumberOfFailedCalls());
        circuitBreakerDetails.setNotPermittedCalls(metrics.getNumberOfNotPermittedCalls());
        circuitBreakerDetails.setState(circuitBreaker.getState());
        return circuitBreakerDetails;
    }

}
