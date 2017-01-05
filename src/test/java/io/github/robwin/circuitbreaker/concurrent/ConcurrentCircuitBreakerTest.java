/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.robwin.circuitbreaker.concurrent;

import com.google.testing.threadtester.*;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;
import io.github.robwin.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.robwin.circuitbreaker.internal.CircuitBreakerStateMachine;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.time.Duration;

import static io.github.robwin.circuitbreaker.CircuitBreaker.*;
import static io.github.robwin.circuitbreaker.event.CircuitBreakerEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentCircuitBreakerTest {

    private CircuitBreakerStateMachine circuitBreaker;
    private TestSubscriber<CircuitBreakerEvent.Type> errorEventSubscriber;
    private TestSubscriber<CircuitBreaker.StateTransition> stateTransitionSubsriber;

    @Test
    public void concurrentConcurrentCircuitBreakerTest() {
        AnnotatedTestRunner runner = new AnnotatedTestRunner();
        runner.runTests(getClass(), CircuitBreakerStateMachine.class);
    }


    @ThreadedBefore
    public void setUp() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(1)
                .build();

        circuitBreaker = (CircuitBreakerStateMachine) of("testName", circuitBreakerConfig);
        errorEventSubscriber = circuitBreaker.getEventStream()
            .filter(event -> event.getEventType() == Type.ERROR)
            .map(CircuitBreakerEvent::getEventType)
            .test();

        stateTransitionSubsriber = circuitBreaker.getEventStream()
            .filter(event -> event.getEventType() == Type.STATE_TRANSITION)
            .cast(CircuitBreakerOnStateTransitionEvent.class)
            .map(CircuitBreakerOnStateTransitionEvent::getStateTransition)
            .test();
    }

    @ThreadedMain
    public void firstActor() {
        circuitBreaker.onError(Duration.ZERO, new RuntimeException());
    }

    @ThreadedSecondary
    public void secondActor() {
        circuitBreaker.onError(Duration.ZERO, new RuntimeException());
    }

    @ThreadedAfter
    public void arbiter() {
        assertThat(circuitBreaker.getState()).isEqualTo(State.OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        errorEventSubscriber
            .assertValues(Type.ERROR, Type.ERROR);
        stateTransitionSubsriber
                .assertValue(StateTransition.CLOSED_TO_OPEN);

    }
}
