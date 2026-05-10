/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for CircuitBreakerUpdateStateResponse DTO
 */
class CircuitBreakerUpdateStateResponseTest {

    @Test
    void equals() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");


        // Verify the results
        assertThat(response2.getMessage()).isEqualTo(response1.getMessage());
        assertThat(response2.getCircuitBreakerName()).isEqualTo(response1.getCircuitBreakerName());
        assertThat(response2.getCurrentState()).isEqualTo(response1.getCurrentState());
        assertThat(response2).isEqualTo(response1);
    }

    @Test
    void testHashCode() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");

        // Verify the results
        Assertions.assertThat(response2).hasSameHashCodeAs(response1);
    }


    @Test
    void testToString() {
        // Setup
        CircuitBreakerUpdateStateResponse response1 = new CircuitBreakerUpdateStateResponse();
        response1.setCircuitBreakerName("test2");
        response1.setCurrentState("TESTState");
        response1.setMessage("TestMessage");
        CircuitBreakerUpdateStateResponse response2 = new CircuitBreakerUpdateStateResponse();
        response2.setCircuitBreakerName("test2");
        response2.setCurrentState("TESTState");
        response2.setMessage("TestMessage");
        // Verify the results
        Assertions.assertThat(response2).hasToString(response1.toString());
    }
}