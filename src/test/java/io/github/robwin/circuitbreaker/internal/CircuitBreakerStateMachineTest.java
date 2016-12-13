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
package io.github.robwin.circuitbreaker.internal;

import static java.lang.Thread.sleep;
import static java.time.Duration.ZERO;
import static org.assertj.core.api.BDDAssertions.assertThat;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;
import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
public class CircuitBreakerStateMachineTest {

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        circuitBreaker = new CircuitBreakerStateMachine("testName", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build());
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test
    public void testCircuitBreakerStateMachine() throws InterruptedException {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        // Call 1 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 3 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 4 is a success
        circuitBreaker.onSuccess(ZERO);
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(4);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 5 is a success
        circuitBreaker.onSuccess(ZERO);
        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(60.0f);

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
        circuitBreaker.onError(ZERO, new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException());
        // Call 3 is a success
        circuitBreaker.onSuccess(ZERO);

        // The ring buffer is filled and the failure rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(50f);

        sleep(1300);

        // The CircuitBreaker switches to half open, because the wait duration of 1 second is elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Call 1 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException());
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 is a success
        circuitBreaker.onSuccess(ZERO);
        // Call 3 is a success
        circuitBreaker.onSuccess(ZERO);

        // The ring buffer is filled and the failure rate is below 50%
        // The state machine transitions back to CLOSED state
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);
    }

    @Test
    public void consumeEvents() throws ExecutionException, InterruptedException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(100)
            .ringBufferSizeInClosedState(2)
            .ringBufferSizeInHalfOpenState(1)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .recordFailure(error -> !(error instanceof NumberFormatException))
            .build();
        circuitBreaker = new CircuitBreakerStateMachine("testName", config);

        int capacity = 5;
        CompletableFuture<ArrayList<String>> future = subscribeOnAllEventsDescriptions(capacity);
        CompletableFuture.runAsync(() -> {
            circuitBreaker.onSuccess(ZERO);
            circuitBreaker.onError(ZERO, new RuntimeException());
            circuitBreaker.onError(ZERO, new NumberFormatException());
            circuitBreaker.onError(ZERO, new RuntimeException());
            circuitBreaker.onError(ZERO, new RuntimeException());
        });

        List<String> buffer = future.get();
        assertThat(buffer).hasSize(capacity);

        Optional<String> ignoredError = buffer.stream()
            .filter(event -> event.contains("has ignored an error: 'java.lang.NumberFormatException'"))
            .findAny();
        assertThat(ignoredError.isPresent()).isTrue();

        Optional<String> recordedError = buffer.stream()
            .filter(event -> event.contains("recorded an error: 'java.lang.RuntimeException'"))
            .findAny();
        assertThat(recordedError.isPresent()).isTrue();

        Optional<String> changedState = buffer.stream()
            .filter(event -> event.contains("changed state"))
            .findAny();
        assertThat(changedState.isPresent()).isTrue();

        Optional<String> success = buffer.stream()
            .filter(event -> event.contains("recorded a successful call"))
            .findAny();
        assertThat(success.isPresent()).isTrue();
    }

    private CompletableFuture<ArrayList<String>> subscribeOnAllEventsDescriptions(final int capacity) {
        Flowable<CircuitBreakerEvent> eventStream = circuitBreaker.getEventStream();
        CompletableFuture<ArrayList<String>> future = new CompletableFuture<>();
        eventStream
            .take(capacity)
            .map(Object::toString)
            .collectInto(new ArrayList<String>(capacity), ArrayList::add)
            .subscribe(future::complete);
        return future;
    }
}
