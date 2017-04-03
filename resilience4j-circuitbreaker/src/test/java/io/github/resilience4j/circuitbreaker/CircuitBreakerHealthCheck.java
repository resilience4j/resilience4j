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
package io.github.resilience4j.circuitbreaker;

import com.codahale.metrics.health.HealthCheck;

public class CircuitBreakerHealthCheck extends HealthCheck {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerHealthCheck(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public HealthCheck.Result check() throws Exception {
        CircuitBreaker.State state = circuitBreakerRegistry.circuitBreaker("testName").getState();
        switch(state){
            case CLOSED: return HealthCheck.Result.healthy();
            case HALF_OPEN: return HealthCheck.Result.healthy();
            default: return HealthCheck.Result.unhealthy(String.format("CircuitBreaker '%s' is OPEN.", "testName"));
        }
    }
}