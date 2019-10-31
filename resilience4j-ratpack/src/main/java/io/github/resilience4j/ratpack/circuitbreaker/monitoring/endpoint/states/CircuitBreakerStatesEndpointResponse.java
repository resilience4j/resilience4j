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

import java.util.List;

public class CircuitBreakerStatesEndpointResponse {

    private List<CircuitBreakerStateDTO> circuitBreakerStates;

    public CircuitBreakerStatesEndpointResponse() {
    }

    public CircuitBreakerStatesEndpointResponse(List<CircuitBreakerStateDTO> circuitBreakerStates) {
        this.circuitBreakerStates = circuitBreakerStates;
    }

    public List<CircuitBreakerStateDTO> getCircuitBreakerStates() {
        return circuitBreakerStates;
    }

    public void setCircuitBreakerStates(List<CircuitBreakerStateDTO> circuitBreakerStates) {
        this.circuitBreakerStates = circuitBreakerStates;
    }
}
