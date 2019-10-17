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
package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * {@link Endpoint} to expose CircuitBreaker events.
 */
@ConfigurationProperties(prefix = "endpoints.circuitbreaker")
public class CircuitBreakerEndpoint extends AbstractEndpoint {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEndpoint(CircuitBreakerRegistry circuitBreakerRegistry) {
        super("circuitbreaker");
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public ResponseEntity<CircuitBreakerEndpointResponse> invoke() {
        List<String> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers()
            .map(CircuitBreaker::getName).sorted().toJavaList();
        return ResponseEntity.ok(new CircuitBreakerEndpointResponse(circuitBreakers));
    }
}
