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
package io.github.resilience4j.circuitbreaker.internal;

import com.statemachinesystems.mockclock.MockClock;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerStateMachineTest {

    private CircuitBreaker circuitBreaker;
    private MockClock mockClock;

    @Before
    public void setUp() {
        mockClock = MockClock.at(2019, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
        circuitBreaker = new CircuitBreakerStateMachine("testName", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(4)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .recordFailure(error -> !(error instanceof NumberFormatException))
                .build(), mockClock);
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test()
    public void shouldThrowCallNotPermittedExceptionWhenStateIsOpen() {
        circuitBreaker.transitionToOpenState();
        assertThatThrownBy(circuitBreaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

    @Test()
    public void shouldThrowCallNotPermittedExceptionWhenStateIsForcedOpen() {
        circuitBreaker.transitionToForcedOpenState();
        assertThatThrownBy(circuitBreaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

    @Test()
    public void shouldIncreaseCounterOnReleasePermission() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);

        circuitBreaker.releasePermission();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
    }

    @Test
    public void shouldThrowCallNotPermittedExceptionWhenNotFurtherTestCallsArePermitted() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        circuitBreaker.tryAcquirePermission();
        circuitBreaker.tryAcquirePermission();
        circuitBreaker.tryAcquirePermission();
        circuitBreaker.tryAcquirePermission();
        assertThatThrownBy(circuitBreaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldOnlyAllowFourTestRequests() {
        assertThatMetricsAreReset();
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.tryAcquirePermission();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.tryAcquirePermission();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
    }

    @Test
    public void shouldOpenAfterRingBufferIsFull() {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatMetricsAreReset();

        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 1, 5, 1, 0L);

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 2, 5, 2, 0L);

        // Call 3 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 3, 5, 3, 0L);

        // Call 4 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 1, 4, 5, 3, 0L);

        // Call 5 is a success
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent

        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(60.0f);
        assertCircuitBreakerMetricsEqualTo(60.0f, 2, 5, 5, 3, 0L);

        // Call to tryAcquirePermission records a notPermittedCall
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertCircuitBreakerMetricsEqualTo(60.0f, 2, 5, 5, 3, 1L);
    }

    @Test
    public void shouldTransitionToHalfOpenAfterWaitDuration() {
        // Initially the CircuitBreaker is open
        circuitBreaker.transitionToOpenState();
        assertThatMetricsAreReset();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker is still open, because the wait duration of 5 seconds is not elapsed.
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 5, 0, 2L);

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker switches to half open, because the wait duration of 5 seconds is elapsed.
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 4, 0, 0L);
    }

    @Test
    public void shouldTransitionToHalfOpenAfterWaitInterval() {
        CircuitBreaker intervalCircuitBreaker = new CircuitBreakerStateMachine("testName", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .ringBufferSizeInClosedState(5)
            .ringBufferSizeInHalfOpenState(4)
            .waitIntervalFunctionInOpenState(IntervalFunction.ofExponentialBackoff(5000L))
            .recordFailure(error -> !(error instanceof NumberFormatException))
            .build(), mockClock);

        // Initially the CircuitBreaker is open
        intervalCircuitBreaker.transitionToOpenState();
        assertThatMetricsAreReset();

        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker is still open, because the wait duration of 5 seconds is not elapsed.
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 5, 0, 2L);

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker switches to half open, because the wait duration of 5 seconds is elapsed.
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 4, 0, 0L);

        intervalCircuitBreaker.transitionToOpenState();
        mockClock.advanceBySeconds(6);
        // The CircuitBreaker is still open, because the new wait duration of 7.5 seconds is not elapsed.
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(5);
        // The CircuitBreaker switches to half open, because the new wait duration of 10 seconds is elapsed.
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 4, 0, 0L);
    }

    @Test
    public void shouldTransitionBackToOpenStateWhenFailureIsAboveThreshold() {
        // Initially the CircuitBreaker is half_open
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 4, 0, 0L);

        // A ring buffer with size 3 is used in half open state
        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        // Call 3 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent (12)
        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // The ring buffer is filled and the failure rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (13)
        assertCircuitBreakerMetricsEqualTo(75f, 1, 4, 4, 3, 0L);
    }


    @Test
    public void shouldTransitionBackToClosedStateWhenFailureIsBelowThreshold() {
        // Initially the CircuitBreaker is half_open
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 4, 0, 0L);

        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // Call 2 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent

        // Call 3 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent

        // Call 4 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent

        // The ring buffer is filled and the failure rate is below 50%
        // The state machine transitions back to CLOSED state
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // Should create a CircuitBreakerOnStateTransitionEvent
        assertCircuitBreakerMetricsEqualTo(-1f, 3, 4, 5, 1, 0L);

        // // Call 5 is a success and fills the buffer in closed state
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should create a CircuitBreakerOnSuccessEvent
        assertCircuitBreakerMetricsEqualTo(20.0f, 4, 5, 5, 1, 0L);

    }

    @Test
    public void shouldResetMetrics() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // Should create a CircuitBreakerOnStateTransitionEvent (21)
        assertThatMetricsAreReset();

        circuitBreaker.onSuccess(0);
        circuitBreaker.onError(0, new RuntimeException());

        assertCircuitBreakerMetricsEqualTo(-1f, 1, 2, 5, 1, 0L);

        circuitBreaker.reset(); // Should create a CircuitBreakerOnResetEvent (20)
        assertThatMetricsAreReset();

    }

    @Test
    public void shouldDisableCircuitBreaker() {
        circuitBreaker.transitionToDisabledState(); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.DISABLED); // Should create a CircuitBreakerOnStateTransitionEvent (21)
        assertThatMetricsAreReset();

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0); // Should not create a CircuitBreakerOnSuccessEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        circuitBreaker.acquirePermission();
        circuitBreaker.onError(0, new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThatMetricsAreReset();
    }

    @Test
    public void shouldForceOpenCircuitBreaker() {
        circuitBreaker.transitionToForcedOpenState(); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);

        mockClock.advanceBySeconds(6);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);

        // The CircuitBreaker should not transition to half open, even if the wait duration of 5 seconds is elapsed.

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 4, 0, 2L);
    }

    private void assertCircuitBreakerMetricsEqualTo(Float expectedFailureRate, Integer expectedSuccessCalls, Integer expectedBufferedCalls, Integer expectedMaxBufferedCalls, Integer expectedFailedCalls, Long expectedNotPermittedCalls) {
        assertCircuitBreakerMetricsEqualTo(circuitBreaker, expectedFailureRate, expectedSuccessCalls, expectedBufferedCalls, expectedMaxBufferedCalls, expectedFailedCalls, expectedNotPermittedCalls);
    }

    private void assertCircuitBreakerMetricsEqualTo(CircuitBreaker toTest, Float expectedFailureRate, Integer expectedSuccessCalls, Integer expectedBufferedCalls, Integer expectedMaxBufferedCalls, Integer expectedFailedCalls, Long expectedNotPermittedCalls) {
        final CircuitBreaker.Metrics metrics = toTest.getMetrics();
        assertThat(metrics.getFailureRate()).isEqualTo(expectedFailureRate);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(expectedSuccessCalls);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(expectedBufferedCalls);
        assertThat(metrics.getMaxNumberOfBufferedCalls()).isEqualTo(expectedMaxBufferedCalls);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(expectedFailedCalls);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(expectedNotPermittedCalls);
    }

    private void assertThatMetricsAreReset() {
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 5, 0, 0L);
    }

}
