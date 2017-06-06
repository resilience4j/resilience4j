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
package io.github.resilience4j.circuitbreaker.concurrent;

import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import io.reactivex.subscribers.TestSubscriber;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.StringResult1;

import java.text.MessageFormat;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.of;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;

@JCStressTest
@org.openjdk.jcstress.annotations.State
@Outcome(
    id = "state=OPEN failed=1 buffered=1" +
        " events=\\[\\[ERROR, ERROR\\], \\[\\], \\[\\]\\]" +
        " transition=\\[\\[State transition from CLOSED to OPEN\\], \\[\\], \\[\\]\\]",
    expect = Expect.ACCEPTABLE
)
public class ConcurrentCircuitBreakerTest {

    private CircuitBreakerStateMachine circuitBreaker;
    private TestSubscriber<Type> errorEventSubscriber;
    private TestSubscriber<StateTransition> stateTransitionSubsriber;

    public ConcurrentCircuitBreakerTest() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(1)
            .build();

        circuitBreaker = (CircuitBreakerStateMachine) of("testName", circuitBreakerConfig);
        errorEventSubscriber = RxJava2Adapter.toFlowable(circuitBreaker.getEventPublisher())
            .filter(event -> event.getEventType() == Type.ERROR)
            .map(CircuitBreakerEvent::getEventType)
            .test();

        stateTransitionSubsriber = RxJava2Adapter.toFlowable(circuitBreaker.getEventPublisher())
            .filter(event -> event.getEventType() == Type.STATE_TRANSITION)
            .cast(CircuitBreakerOnStateTransitionEvent.class)
            .map(CircuitBreakerOnStateTransitionEvent::getStateTransition)
            .test();
    }

    @Actor
    public void firstActor() {
        circuitBreaker.onError(0, new RuntimeException());
    }

    @Actor
    public void secondActor() {
        circuitBreaker.onError(0, new RuntimeException());
    }

    @Arbiter
    public void arbiter(StringResult1 result1) {
        String result = MessageFormat.format(
            "state={0} failed={1} buffered={2} events={3} transition={4}",
            circuitBreaker.getState(),
            circuitBreaker.getMetrics().getNumberOfFailedCalls(),
            circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
            errorEventSubscriber.getEvents(),
            stateTransitionSubsriber.getEvents()
        );
        result1.r1 = result;
    }
}
