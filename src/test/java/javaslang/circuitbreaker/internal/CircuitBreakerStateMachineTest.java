/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.internal;

import javaslang.circuitbreaker.CircuitBreaker;
import javaslang.circuitbreaker.CircuitBreakerConfig;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerStateMachineTest {

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        circuitBreaker = new CircuitBreakerStateMachine("testName", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build());
    }

    @Test
    public void shouldReturnTheCorrectName(){
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test
    public void testCircuitBreakerStateMachine() throws InterruptedException {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 1 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 2 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 3 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 4 is a success
        circuitBreaker.recordSuccess();
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 5 is a success
        circuitBreaker.recordSuccess();
        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        sleep(500);

        // The CircuitBreaker is still open, because the wait duration of 1 second is not elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        sleep(800);

        // The CircuitBreaker switches to half open, because the wait duration of 1 second is elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // A ring buffer with size 2 is used in half open state
        // Call 1 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Call 2 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        // The ring buffer is filled and the failure rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        sleep(1300);

        // The CircuitBreaker switches to half open, because the wait duration of 1 second is elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Call 1 is a failure
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Call 2 is a success
        circuitBreaker.recordSuccess();
        // The ring buffer is filled and the failure rate is equal to 50%
        // The state machine transitions back to CLOSED state
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
