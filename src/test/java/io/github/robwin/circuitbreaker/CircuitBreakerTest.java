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
package io.github.robwin.circuitbreaker;

import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        circuitBreaker = new DefaultCircuitBreaker("testName", new CircuitBreakerConfig.Builder().maxFailures(2).waitInterval(1000).build());
    }

    @Test
    public void shouldReturnTheCorrectName(){
        assertThat(circuitBreaker.toString()).isEqualTo("CircuitBreaker 'testName'");
    }

    @Test
    public void testCircuitBreaker() throws InterruptedException {
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 1
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 2
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure();  // failure 3
        assertThat(circuitBreaker.isClosed()).isEqualTo(false);  // open after third failure
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        sleep(500);
        assertThat(circuitBreaker.isClosed()).isEqualTo(false); // still open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // still open
        sleep(800);
        assertThat(circuitBreaker.isClosed()).isEqualTo(true); // half-closed after more than 1 second
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_CLOSED);
        circuitBreaker.recordFailure(); // but backend still unavailable
        assertThat(circuitBreaker.isClosed()).isEqualTo(false); // back to open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        sleep(1300);
        assertThat(circuitBreaker.isClosed()).isEqualTo(true); // half-closed after more than 1 second
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_CLOSED);
        circuitBreaker.recordSuccess();
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // closed again and failure count is reset
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);  // closed because failure count was reset
    }
}
