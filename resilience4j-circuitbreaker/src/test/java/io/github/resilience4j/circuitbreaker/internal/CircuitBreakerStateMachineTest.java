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
import io.github.resilience4j.circuitbreaker.IllegalStateTransitionException;
import io.github.resilience4j.circuitbreaker.event.*;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.IntervalFunction;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.FORCED_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CircuitBreakerStateMachineTest {

    private CircuitBreaker circuitBreaker;
    private MockClock mockClock;
    private EventConsumer<CircuitBreakerOnSuccessEvent> mockOnSuccessEventConsumer;
    private EventConsumer<CircuitBreakerOnErrorEvent> mockOnErrorEventConsumer;
    private EventConsumer<CircuitBreakerOnStateTransitionEvent> mockOnStateTransitionEventConsumer;
    private EventConsumer<CircuitBreakerOnFailureRateExceededEvent> mockOnFailureRateExceededEventConsumer;
    private EventConsumer<CircuitBreakerOnSlowCallRateExceededEvent> mockOnSlowCallRateExceededEventConsumer;

//    TODO: add tests here for record Result

    @Before
    public void setUp() {
        mockOnSuccessEventConsumer = (EventConsumer<CircuitBreakerOnSuccessEvent>) mock(EventConsumer.class);
        mockOnErrorEventConsumer = (EventConsumer<CircuitBreakerOnErrorEvent>) mock(EventConsumer.class);
        mockOnStateTransitionEventConsumer = (EventConsumer<CircuitBreakerOnStateTransitionEvent>) mock(EventConsumer.class);
        mockOnFailureRateExceededEventConsumer = (EventConsumer<CircuitBreakerOnFailureRateExceededEvent>) mock(EventConsumer.class);
        mockOnSlowCallRateExceededEventConsumer = (EventConsumer<CircuitBreakerOnSlowCallRateExceededEvent>) mock(EventConsumer.class);
        mockClock = MockClock.at(2019, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
        circuitBreaker = new CircuitBreakerStateMachine("testName", custom()
            .failureRateThreshold(50)
            .permittedNumberOfCallsInHalfOpenState(4)
            .slowCallDurationThreshold(Duration.ofSeconds(4))
            .slowCallRateThreshold(50)
            .maxWaitDurationInHalfOpenState(Duration.ofSeconds(1))
            .slidingWindow(20, 5, SlidingWindowType.TIME_BASED)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .ignoreExceptions(NumberFormatException.class)
            .currentTimestampFunction(clock -> clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS)
            .build(), mockClock);
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test()
    public void shouldThrowCallNotPermittedExceptionWhenStateIsOpen() {
        circuitBreaker.transitionToOpenState();
        assertThatThrownBy(circuitBreaker::acquirePermission)
            .isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

    @Test()
    public void shouldThrowCallNotPermittedExceptionWhenStateIsForcedOpen() {
        circuitBreaker.transitionToForcedOpenState();
        assertThatThrownBy(circuitBreaker::acquirePermission)
            .isInstanceOf(CallNotPermittedException.class);
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
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThatThrownBy(circuitBreaker::acquirePermission)
            .isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldOnlyPermitFourCallsInHalfOpenState() {
        assertThatMetricsAreReset();
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
    }


    @Test
    public void shouldOpenAfterFailureRateThresholdExceeded2() {
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);

        mockClock.advanceBySeconds(1);

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);

        mockClock.advanceBySeconds(1);

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);

        mockClock.advanceBySeconds(1);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        mockClock.advanceBySeconds(1);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        mockClock.advanceBySeconds(1);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldOpenAfterFailureRateThresholdExceeded() {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatMetricsAreReset();
        circuitBreaker.getEventPublisher().onFailureRateExceeded(mockOnFailureRateExceededEventConsumer);

        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 1, 1, 0L);

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 2, 2, 0L);

        // Call 3 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 3, 3, 0L);

        // Call 4 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 1, 4, 3, 0L);

        // Call 5 is a success
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent

        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(60.0f);
        assertCircuitBreakerMetricsEqualTo(60.0f, 2, 5, 3, 0L);
        verify(mockOnFailureRateExceededEventConsumer, times(1)).consumeEvent(any(CircuitBreakerOnFailureRateExceededEvent.class));

        // Call to tryAcquirePermission records a notPermittedCall
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);
        assertCircuitBreakerMetricsEqualTo(60.0f, 2, 5, 3, 1L);
    }

    @Test
    public void shouldOpenAfterSlowCallRateThresholdExceeded() {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatMetricsAreReset();

        // Call 1 is slow
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(5, TimeUnit.SECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 2 is slow
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(5, TimeUnit.SECONDS); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 3 is fast
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(100, TimeUnit.MILLISECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 4 is fast
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(100, TimeUnit.MILLISECONDS); // Should create a CircuitBreakerOnSuccessEvent
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 5 is slow
        circuitBreaker
            .onSuccess(5, TimeUnit.SECONDS); // Should create a CircuitBreakerOnSuccessEvent

        // The ring buffer is filled and the slow call rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getSlowCallRate()).isEqualTo(60.0f);

    }

    @Test
    public void shouldOpenAfterSlowCallRateThresholdExceededUsingMockClock() {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatMetricsAreReset();
        Consumer<Integer> consumer = circuitBreaker.decorateConsumer(mockClock::advanceBySeconds);
        // Call 1 is slow
        consumer.accept(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 2 is slow
        consumer.accept(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 3 is fast
        consumer.accept(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 4 is fast
        consumer.accept(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(4);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Call 5 is slow
        consumer.accept(5);
        // The ring buffer is filled and the slow call rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getSlowCallRate()).isEqualTo(60.0f);
    }

    @Test
    public void shouldTransitionToHalfOpenAfterWaitDuration() {
        // Initially the CircuitBreaker is open
        circuitBreaker.transitionToOpenState();
        assertThatMetricsAreReset();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission())
            .isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker is still open, because the wait duration of 5 seconds is not elapsed.
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission())
            .isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 2L);

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker switches to half open, because the wait duration of 5 seconds is elapsed.
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);
    }

    @Test
    public void shouldTransitionToHalfOpenAfterWaitInterval() {
        CircuitBreaker intervalCircuitBreaker = new CircuitBreakerStateMachine("testName",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(5)
                .permittedNumberOfCallsInHalfOpenState(4)
                .waitIntervalFunctionInOpenState(IntervalFunction.ofExponentialBackoff(5000L))
                .recordException(error -> !(error instanceof NumberFormatException))
                .build(), mockClock);

        // Initially the CircuitBreaker is open
        intervalCircuitBreaker.transitionToOpenState();
        assertThatMetricsAreReset();

        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission())
            .isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker is still open, because the wait duration of 5 seconds is not elapsed.
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission())
            .isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 0, 2L);

        mockClock.advanceBySeconds(3);

        // The CircuitBreaker switches to half open, because the wait duration of 5 seconds is elapsed.
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 0, 0L);

        intervalCircuitBreaker.transitionToOpenState();
        mockClock.advanceBySeconds(6);
        // The CircuitBreaker is still open, because the new wait duration of 7.5 seconds is not elapsed.
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(intervalCircuitBreaker.tryAcquirePermission())
            .isEqualTo(false); // Should create a CircuitBreakerOnCallNotPermittedEvent

        mockClock.advanceBySeconds(5);
        // The CircuitBreaker switches to half open, because the new wait duration of 10 seconds is elapsed.
        assertThat(intervalCircuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(intervalCircuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.HALF_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (9)
        // Metrics are reset
        assertCircuitBreakerMetricsEqualTo(intervalCircuitBreaker, -1f, 0, 0, 0, 0L);
    }

    @Test
    public void shouldTransitionBackToOpenStateWhenFailureRateIsAboveThreshold() {
        // Initially the CircuitBreaker is half_open
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);

        // A ring buffer with size 3 is used in half open state
        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent
        // Call 3 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0,
            TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent (12)
        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // The ring buffer is filled and the failure rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (13)
        assertCircuitBreakerMetricsEqualTo(75f, 1, 4, 3, 0L);
    }

    @Test
    public void shouldTransitionBackToOpenStateWhenSlowCallRateIsAboveThreshold() {
        // Initially the CircuitBreaker is half_open
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Call 1 is slow
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(5, TimeUnit.SECONDS);

        // Call 2 is slow
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(5, TimeUnit.SECONDS);

        // Call 3 is slow
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(5, TimeUnit.SECONDS);

        // Call 4 is fast
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(1, TimeUnit.SECONDS);

        // The failure rate is blow 50%, but slow call rate is above 50%
        // The state machine transitions back to OPEN state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getSlowCallRate()).isEqualTo(75.0f);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(0f);
    }


    @Test
    public void shouldTransitionBackToClosedStateWhenFailureIsBelowThreshold() {
        // Initially the CircuitBreaker is half_open
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);

        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should create a CircuitBreakerOnErrorEvent

        // Call 2 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent

        // Call 3 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent

        // Call 4 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent

        // The ring buffer is filled and the failure rate is below 50%
        // The state machine transitions back to CLOSED state
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.CLOSED); // Should create a CircuitBreakerOnStateTransitionEvent
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);

        // // Call 5 is a success and fills the buffer in closed state
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should create a CircuitBreakerOnSuccessEvent
        assertCircuitBreakerMetricsEqualTo(-1f, 1, 1, 0, 0L);

    }

    @Test
    public void shouldResetMetrics() {
        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.CLOSED); // Should create a CircuitBreakerOnStateTransitionEvent (21)
        assertThatMetricsAreReset();

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        assertCircuitBreakerMetricsEqualTo(-1f, 1, 2, 1, 0L);

        circuitBreaker.reset(); // Should create a CircuitBreakerOnResetEvent (20)
        assertThatMetricsAreReset();

    }

    @Test
    public void shouldDisableCircuitBreaker() {
        circuitBreaker
            .transitionToDisabledState(); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.getState()).isEqualTo(
            CircuitBreaker.State.DISABLED); // Should create a CircuitBreakerOnStateTransitionEvent (21)
        assertThatMetricsAreReset();

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker
            .onSuccess(0, TimeUnit.NANOSECONDS); // Should not create a CircuitBreakerOnSuccessEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        circuitBreaker.acquirePermission();
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS,
            new RuntimeException()); // Should not create a CircuitBreakerOnErrorEvent

        assertThatMetricsAreReset();
    }

    @Test
    public void shouldForceOpenCircuitBreaker() {
        circuitBreaker
            .transitionToForcedOpenState(); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.getState()).isEqualTo(
            FORCED_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);

        mockClock.advanceBySeconds(6);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(false);

        // The CircuitBreaker should not transition to half open, even if the wait duration of 5 seconds is elapsed.

        assertThat(circuitBreaker.getState()).isEqualTo(
            FORCED_OPEN); // Should create a CircuitBreakerOnStateTransitionEvent
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 2L);
    }

    @Test
    public void shouldReleasePermissionWhenExceptionIgnored() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 1, 1, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 2, 2, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS); //
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 3, 3, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        // Should ignore NumberFormatException and release permission
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertCircuitBreakerMetricsEqualTo(-1f, 3, 3, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);
    }

    @Test
    public void shouldIgnoreExceptionsAndThenTransitionToClosed() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        // Should ignore NumberFormatException and release permission
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        // Should ignore NumberFormatException and release permission
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        // Should ignore NumberFormatException and release permission
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        // Should ignore NumberFormatException and release permission
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new NumberFormatException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS); //
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS); //
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS); //
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        assertCircuitBreakerMetricsEqualTo(-1f, 3, 3, 0, 0L);

        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS); //
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);
    }


    @Test
    public void shouldNotAllowTransitionFromClosedToHalfOpen() {
        assertThatThrownBy(() -> circuitBreaker.transitionToHalfOpenState())
            .isInstanceOf(IllegalStateTransitionException.class)
            .hasMessage(
                "CircuitBreaker 'testName' tried an illegal state transition from CLOSED to HALF_OPEN");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldResetToClosedState() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.reset();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldResetClosedState() {
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);

        circuitBreaker.reset();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldResetMetricsAfterMetricsOnlyStateTransition() {
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);

        circuitBreaker.transitionToMetricsOnlyState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldRecordMetricsInMetricsOnlyState() {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreaker is closed
        circuitBreaker.transitionToMetricsOnlyState();
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertThatMetricsAreReset();
        circuitBreaker.getEventPublisher().onSuccess(mockOnSuccessEventConsumer);
        circuitBreaker.getEventPublisher().onError(mockOnErrorEventConsumer);
        circuitBreaker.getEventPublisher().onStateTransition(mockOnStateTransitionEventConsumer);
        circuitBreaker.getEventPublisher().onFailureRateExceeded(mockOnFailureRateExceededEventConsumer);

        // Call 1 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        verify(mockOnErrorEventConsumer, times(1)).consumeEvent(any(CircuitBreakerOnErrorEvent.class));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 1, 1, 0L);

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        verify(mockOnErrorEventConsumer, times(2)).consumeEvent(any(CircuitBreakerOnErrorEvent.class));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 2, 2, 0L);

        // Call 3 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        verify(mockOnErrorEventConsumer, times(3)).consumeEvent(any(CircuitBreakerOnErrorEvent.class));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 3, 3, 0L);

        // Call 4 is a success
        assertThat(circuitBreaker.tryAcquirePermission()).isEqualTo(true);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        verify(mockOnSuccessEventConsumer, times(1)).consumeEvent(any(CircuitBreakerOnSuccessEvent.class));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertCircuitBreakerMetricsEqualTo(-1f, 1, 4, 3, 0L);

        // Call 5 is a success
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        verify(mockOnSuccessEventConsumer, times(2)).consumeEvent(any(CircuitBreakerOnSuccessEvent.class));

        // The ring buffer is filled and the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        verify(mockOnStateTransitionEventConsumer, never()).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        verify(mockOnFailureRateExceededEventConsumer, times(1)).consumeEvent(any(CircuitBreakerOnFailureRateExceededEvent.class));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(60.0f);
        assertCircuitBreakerMetricsEqualTo(60.0f, 2, 5, 3, 0L);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        verify(mockOnFailureRateExceededEventConsumer, times(1)).consumeEvent(any(CircuitBreakerOnFailureRateExceededEvent.class));
        verify(mockOnErrorEventConsumer, times(4)).consumeEvent(any(CircuitBreakerOnErrorEvent.class));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
    }

    @Test
    public void shouldPublishEachEventOnceInMetricOnlyState() {
        circuitBreaker.transitionToMetricsOnlyState();
        circuitBreaker.getEventPublisher().onFailureRateExceeded(mockOnFailureRateExceededEventConsumer);
        circuitBreaker.getEventPublisher().onSlowCallRateExceeded(mockOnSlowCallRateExceededEventConsumer);

        // trigger FailureRateExceededEvent and SlowCallRateExceededEvent
        for(int times = 0; times<5; times++) {
            circuitBreaker.onError(5, TimeUnit.SECONDS, new RuntimeException());
        }

        verify(mockOnFailureRateExceededEventConsumer, times(1))
            .consumeEvent(any(CircuitBreakerOnFailureRateExceededEvent.class));
        verify(mockOnSlowCallRateExceededEventConsumer, times(1))
            .consumeEvent(any(CircuitBreakerOnSlowCallRateExceededEvent.class));
    }

    @Test
    public void allCircuitBreakerStatesAllowTransitionToMetricsOnlyMode() {
        for (final CircuitBreaker.State state : CircuitBreaker.State.values()) {
            CircuitBreaker.StateTransition.transitionBetween(circuitBreaker.getName(), state, CircuitBreaker.State.METRICS_ONLY);
        }
    }

    @Test
    public void allCircuitBreakerStatesAllowTransitionToItsOwnState() {
        for (final CircuitBreaker.State state : CircuitBreaker.State.values()) {
            CircuitBreaker.StateTransition.transitionBetween(circuitBreaker.getName(), state, state);
        }
    }

    @Test
    public void circuitBreakerDoesNotPublishStateTransitionEventsForInternalTransitions() {
        circuitBreaker.getEventPublisher().onStateTransition(mockOnStateTransitionEventConsumer);
        int expectedNumberOfStateTransitions = 0;

        circuitBreaker.transitionToOpenState();
        expectedNumberOfStateTransitions++;
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        circuitBreaker.transitionToOpenState();
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));

        circuitBreaker.transitionToHalfOpenState();
        expectedNumberOfStateTransitions++;
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        circuitBreaker.transitionToHalfOpenState();
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));

        circuitBreaker.transitionToDisabledState();
        expectedNumberOfStateTransitions++;
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        circuitBreaker.transitionToDisabledState();
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));

        circuitBreaker.transitionToMetricsOnlyState();
        expectedNumberOfStateTransitions++;
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        circuitBreaker.transitionToMetricsOnlyState();
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));

        circuitBreaker.transitionToClosedState();
        expectedNumberOfStateTransitions++;
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
        circuitBreaker.transitionToClosedState();
        verify(mockOnStateTransitionEventConsumer, times(expectedNumberOfStateTransitions)).consumeEvent(any(CircuitBreakerOnStateTransitionEvent.class));
    }

    @Test
    public void circuitBreakerTransitionsToOpenAfterWaitDurationInHalfOpenState() throws InterruptedException {
        circuitBreaker.transitionToOpenState();
        // Initially the CircuitBreaker is in Half Open State
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        // sleeping for maxWaitDurationInHalfOpenState to expire (maxWaitDurationInHalfOpenState = 1Sec)
        Thread.sleep(2000l);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private void assertCircuitBreakerMetricsEqualTo(Float expectedFailureRate,
        Integer expectedSuccessCalls, Integer expectedBufferedCalls, Integer expectedFailedCalls,
        Long expectedNotPermittedCalls) {
        assertCircuitBreakerMetricsEqualTo(circuitBreaker, expectedFailureRate,
            expectedSuccessCalls, expectedBufferedCalls, expectedFailedCalls,
            expectedNotPermittedCalls);
    }

    private void assertCircuitBreakerMetricsEqualTo(CircuitBreaker toTest,
        Float expectedFailureRate, Integer expectedSuccessCalls, Integer expectedBufferedCalls,
        Integer expectedFailedCalls, Long expectedNotPermittedCalls) {
        final CircuitBreaker.Metrics metrics = toTest.getMetrics();
        assertThat(metrics.getFailureRate()).isEqualTo(expectedFailureRate);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(expectedSuccessCalls);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(expectedBufferedCalls);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(expectedFailedCalls);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(expectedNotPermittedCalls);
    }

    private void assertThatMetricsAreReset() {
        assertCircuitBreakerMetricsEqualTo(-1f, 0, 0, 0, 0L);
    }

}
