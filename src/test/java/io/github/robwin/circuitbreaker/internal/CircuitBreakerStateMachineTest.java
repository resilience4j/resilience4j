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

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static io.github.robwin.circuitbreaker.event.CircuitBreakerEvent.Type.*;
import static java.lang.Thread.sleep;
import static java.time.Duration.ZERO;
import static org.assertj.core.api.BDDAssertions.assertThat;
public class CircuitBreakerStateMachineTest {

    private CircuitBreaker circuitBreaker;
    private TestSubscriber<CircuitBreakerEvent.Type> testSubscriber;

    @Before
    public void setUp(){
        circuitBreaker = new CircuitBreakerStateMachine("testName", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .recordFailure(error -> !(error instanceof NumberFormatException))
                .build());
        testSubscriber = circuitBreaker.getEventStream()
                .map(CircuitBreakerEvent::getEventType)
                .test();
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
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);

        // Call 1 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (1)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (2)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 3 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (3)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 4 is a success
        circuitBreaker.onSuccess(ZERO); // Should create a CircuitBreakerOnSuccessEvent (4)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(4);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 5 is a success
        circuitBreaker.onSuccess(ZERO); // Should create a CircuitBreakerOnSuccessEvent (5)
        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(60.0f);

        sleep(500);

        // The CircuitBreaker is still open, because the wait duration of 1 second is not elapsed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent (7)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent (8)
        // Two calls are tried, but not permitted, because the CircuitBreaker is open
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(2);

        sleep(800);

        // The CircuitBreaker switches to half open, because the wait duration of 1 second is elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reseted
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        // A ring buffer with size 2 is used in half open state
        // Call 1 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (10)
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (11)
        // Call 3 is a success
        circuitBreaker.onSuccess(ZERO); // Should create a CircuitBreakerOnSuccessEvent (12)

        // The ring buffer is filled and the failure rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (13)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(50f);

        sleep(1300);

        // The CircuitBreaker switches to half open, because the wait duration of 1 second is elapsed
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);  // Should create a CircuitBreakerOnStateTransitionEvent (14)

        // Call 1 is a failure
        circuitBreaker.onError(ZERO, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent (15)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 2 should be ignored, because it's a NumberFormatException
        circuitBreaker.onError(ZERO, new NumberFormatException()); // Should create a CircuitBreakerOnIgnoredErrorEvent (16)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        // Call 3 is a success
        circuitBreaker.onSuccess(ZERO); // Should create a CircuitBreakerOnSuccessEvent (17)
        // Call 43 is a success
        circuitBreaker.onSuccess(ZERO); // Should create a CircuitBreakerOnSuccessEvent (18)

        // The ring buffer is filled and the failure rate is below 50%
        // The state machine transitions back to CLOSED state
        assertThat(circuitBreaker.isCallPermitted()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // Should create a CircuitBreakerOnStateTransitionEvent (19)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(-1f);

        testSubscriber
                .assertValueCount(19)
                .assertValues(ERROR, ERROR, ERROR, SUCCESS, SUCCESS, STATE_TRANSITION,
                        NOT_PERMITTED, NOT_PERMITTED, STATE_TRANSITION,
                        ERROR, ERROR, SUCCESS, STATE_TRANSITION, STATE_TRANSITION,
                        ERROR, IGNORED_ERROR, SUCCESS, SUCCESS, STATE_TRANSITION);
    }
}
