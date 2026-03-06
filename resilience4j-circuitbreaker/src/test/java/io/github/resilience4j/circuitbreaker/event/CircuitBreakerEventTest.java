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
package io.github.resilience4j.circuitbreaker.event;

import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerEventTest {

    @Test
    public void testCircuitBreakerOnErrorEvent() {
        CircuitBreakerOnErrorEvent circuitBreakerEvent = new CircuitBreakerOnErrorEvent("test",
            Duration.ofSeconds(1), new IOException());
        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getElapsedDuration().getSeconds()).isEqualTo(1);
        assertThat(circuitBreakerEvent.getThrowable()).isInstanceOf(IOException.class);
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.ERROR);
        assertThat(circuitBreakerEvent.toString())
            .contains("CircuitBreaker 'test' recorded an error: 'java.io.IOException'.");
    }

    @Test
    public void testCircuitBreakerOnIgnoredErrorEvent() {
        CircuitBreakerOnIgnoredErrorEvent circuitBreakerEvent = new CircuitBreakerOnIgnoredErrorEvent(
            "test",
            Duration.ofSeconds(1), new IOException());

        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getElapsedDuration().getSeconds()).isEqualTo(1);
        assertThat(circuitBreakerEvent.getThrowable()).isInstanceOf(IOException.class);
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.IGNORED_ERROR);
        assertThat(circuitBreakerEvent.toString()).contains(
            "CircuitBreaker 'test' recorded an error which has been ignored: 'java.io.IOException'.");
    }

    @Test
    public void testCircuitBreakerOnStateTransitionEvent() {
        CircuitBreakerOnStateTransitionEvent circuitBreakerEvent = new CircuitBreakerOnStateTransitionEvent(
            "test",
            StateTransition.CLOSED_TO_OPEN);

        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getStateTransition())
            .isEqualTo(StateTransition.CLOSED_TO_OPEN);
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.STATE_TRANSITION);
        assertThat(circuitBreakerEvent.toString())
            .contains("CircuitBreaker 'test' changed state from CLOSED to OPEN");
    }

    @Test
    public void testCircuitBreakerOnResetEvent() {
        CircuitBreakerOnResetEvent circuitBreakerEvent = new CircuitBreakerOnResetEvent("test");

        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.RESET);
        assertThat(circuitBreakerEvent.toString()).contains("CircuitBreaker 'test' reset");
    }

    @Test
    public void testCircuitBreakerOnSuccessEvent() {
        CircuitBreakerOnSuccessEvent circuitBreakerEvent = new CircuitBreakerOnSuccessEvent("test",
            Duration.ofSeconds(1));

        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getElapsedDuration().getSeconds()).isEqualTo(1);
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.SUCCESS);
        assertThat(circuitBreakerEvent.toString())
            .contains("CircuitBreaker 'test' recorded a successful call.");
    }

    @Test
    public void testCircuitBreakerOnCallNotPermittedEvent() {
        CircuitBreakerOnCallNotPermittedEvent circuitBreakerEvent = new CircuitBreakerOnCallNotPermittedEvent(
            "test");

        assertThat(circuitBreakerEvent.getCircuitBreakerName()).isEqualTo("test");
        assertThat(circuitBreakerEvent.getEventType()).isEqualTo(Type.NOT_PERMITTED);
        assertThat(circuitBreakerEvent.toString())
            .contains("CircuitBreaker 'test' recorded a call which was not permitted.");
    }

}
