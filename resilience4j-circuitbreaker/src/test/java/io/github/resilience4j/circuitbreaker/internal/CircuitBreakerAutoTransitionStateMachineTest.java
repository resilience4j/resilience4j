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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerAutoTransitionStateMachineTest {

    private final List<CircuitBreaker> circuitBreakersGroupA = new ArrayList<>();
    private final List<CircuitBreaker> circuitBreakersGroupB = new ArrayList<>();
    private final Map<Integer, Integer> stateTransitionFromOpenToHalfOpen = new HashMap<>();

    private static final int TOTAL_NUMBER_CIRCUIT_BREAKERS = 10;

    @Before
    public void setUp() {
        CircuitBreakerConfig circuitBreakerConfigGroupA = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(3)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .recordFailure(error -> !(error instanceof NumberFormatException))
                .build();

        CircuitBreakerConfig circuitBreakerConfigGroupB = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(5)
                .ringBufferSizeInHalfOpenState(3)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .recordFailure(error -> !(error instanceof NumberFormatException))
                .build();

        // Instantiate multiple circuit breakers in two groups, A & B
        for (int i = 0; i < TOTAL_NUMBER_CIRCUIT_BREAKERS; i++) {

            stateTransitionFromOpenToHalfOpen.put(i, 0);
            // On state transition from OPEN to HALF_OPEN, increment a count
            int finalI = i;
            EventConsumer<CircuitBreakerOnStateTransitionEvent> eventConsumer = transition -> {
                if (transition.getStateTransition().getFromState().equals(CircuitBreaker.State.OPEN) &&
                        transition.getStateTransition().getToState().equals(CircuitBreaker.State.HALF_OPEN)) {
                    Integer currentCount = stateTransitionFromOpenToHalfOpen.get(finalI);
                    stateTransitionFromOpenToHalfOpen.put(finalI, currentCount + 1);
                }
            };

            CircuitBreaker circuitBreaker;
            if (i < TOTAL_NUMBER_CIRCUIT_BREAKERS / 2) {
                circuitBreaker = new CircuitBreakerStateMachine("testNameA" + i, circuitBreakerConfigGroupA);
                circuitBreaker.getEventPublisher().onStateTransition(eventConsumer);
                circuitBreakersGroupA.add(circuitBreaker);
            } else {
                circuitBreaker = new CircuitBreakerStateMachine("testNameB" + i, circuitBreakerConfigGroupB);
                circuitBreaker.getEventPublisher().onStateTransition(eventConsumer);
                circuitBreakersGroupB.add(circuitBreaker);
            }
        }
    }

    @Test
    public void testAutoTransition() throws InterruptedException {
        // A ring buffer with size 5 is used in closed state
        // Initially the CircuitBreakers are closed
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        assertThatAllGroupAMetricsAreReset();

        // Call 1 is a failure
        circuitBreakersGroupA.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (1)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupAMetricsEqualTo(-1f, null, 1, null, 1, 0L);

        // Call 2 is a failure
        circuitBreakersGroupA.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (2)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupAMetricsEqualTo(-1f, null, 2, null, 2, 0L);

        // Call 3 is a failure
        circuitBreakersGroupA.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (3)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupAMetricsEqualTo(-1f, null, 3, null, 3, 0L);

        // Call 4 is a success
        circuitBreakersGroupA.forEach(cb -> cb.onSuccess(0)); // Should create a CircuitBreakerOnSuccessEvent (4)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupAMetricsEqualTo(-1f, null, 4, null, 3, 0L);

        // Call 5 is a success
        circuitBreakersGroupA.forEach(cb -> cb.onSuccess(0)); // Should create a CircuitBreakerOnSuccessEvent (4)
        // The ring buffer is filled and the failure rate is above 50%
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfBufferedCalls(), 5);
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfFailedCalls(), 3);
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getFailureRate(), 60.0f);
        this.assertAllGroupAMetricsEqualTo(60.0f, null, 5, null, 3, 0L);

        sleep(50);

        // Initially the CircuitBreakers are closed
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        assertThatAllGroupBMetricsAreReset();

        // Call 1 is a failure
        circuitBreakersGroupB.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (1)
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupBMetricsEqualTo(-1f, null, 1, null, 1, 0L);

        // Call 2 is a failure
        circuitBreakersGroupB.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (2)
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupBMetricsEqualTo(-1f, null, 2, null, 2, 0L);

        // Call 3 is a failure
        circuitBreakersGroupB.forEach(cb -> cb.onError(0, new RuntimeException())); // Should create a CircuitBreakerOnErrorEvent (3)
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupBMetricsEqualTo(-1f, null, 3, null, 3, 0L);

        // Call 4 is a success
        circuitBreakersGroupB.forEach(cb -> cb.onSuccess(0)); // Should create a CircuitBreakerOnSuccessEvent (4)
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, true);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.CLOSED);
        this.assertAllGroupBMetricsEqualTo(-1f, null, 4, null, 3, 0L);

        // Call 5 is a success
        circuitBreakersGroupB.forEach(cb -> cb.onSuccess(0)); // Should create a CircuitBreakerOnSuccessEvent (4)
        // The ring buffer is filled and the failure rate is above 50%
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.OPEN); // Should create a CircuitBreakerOnStateTransitionEvent (6)
        this.assertAllGroupBCircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfBufferedCalls(), 5);
        this.assertAllGroupBCircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfFailedCalls(), 3);
        this.assertAllGroupBCircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getFailureRate(), 60.0f);
        this.assertAllGroupBMetricsEqualTo(60.0f, null, 5, null, 3, 0L);

        sleep(400);

        // The CircuitBreakers in group A are still open, because the wait duration of 2 seconds is not elapsed
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.OPEN);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (7)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (8)
        // Two calls are tried, but not permitted, because the CircuitBreakers are open
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfNotPermittedCalls(), 2L);

        // The CircuitBreakers in group B are still open, because the wait duration of 1 second is not elapsed
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.OPEN);
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (7)
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (8)
        // Two calls are tried, but not permitted, because the CircuitBreakers are open
        this.assertAllGroupBCircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfNotPermittedCalls(), 2L);

        sleep(650);

        // The CircuitBreakers in group A are still open, because the wait duration of 2 seconds is not elapsed
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.OPEN);
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (9)
        this.assertAllGroupACircuitBreakers(CircuitBreaker::tryObtainPermission, false);  // Should create a CircuitBreakerOnCallNotPermittedEvent (10)
        // Two calls are tried, but not permitted, because the CircuitBreakers are open
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfNotPermittedCalls(), 4L);

        // The CircuitBreakers in Group B switch to half open, because the wait duration of 1 second is elapsed
        this.assertAllGroupBCircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.HALF_OPEN);
        assertThat(stateTransitionFromOpenToHalfOpen.values().stream().filter(count -> count.equals(1)).count()).isEqualTo((long) TOTAL_NUMBER_CIRCUIT_BREAKERS / 2);
        // Metrics are reset
        this.assertAllGroupBCircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfFailedCalls(), 0);
        this.assertAllGroupBMetricsEqualTo(-1f, null, 0, 3, 0, 0L);

        sleep(1000);

        // The CircuitBreakers switch to half open, because the wait duration of 2 second is elapsed
        this.assertAllGroupACircuitBreakers(CircuitBreaker::getState, CircuitBreaker.State.HALF_OPEN);
        assertThat(stateTransitionFromOpenToHalfOpen.values().stream().allMatch(count -> count.equals(1))).isEqualTo(true);
        // Metrics are reset
        this.assertAllGroupACircuitBreakers((CircuitBreaker cb) -> cb.getMetrics().getNumberOfFailedCalls(), 0);
        this.assertAllGroupAMetricsEqualTo(-1f, null, 0, 3, 0, 0L);
    }

    private void assertAllGroupACircuitBreakers(Function<CircuitBreaker, Object> circuitBreakerFunction, Object expected) {
        assertAllCircuitBreakers(circuitBreakersGroupA, circuitBreakerFunction, expected);
    }

    private void assertAllGroupBCircuitBreakers(Function<CircuitBreaker, Object> circuitBreakerFunction, Object expected) {
        assertAllCircuitBreakers(circuitBreakersGroupB, circuitBreakerFunction, expected);
    }

    private void assertAllCircuitBreakers(List<CircuitBreaker> circuitBreakers, Function<CircuitBreaker, Object> circuitBreakerFunction, Object expected) {
        circuitBreakers.forEach(circuitBreaker -> {
            Object result = circuitBreakerFunction.apply(circuitBreaker);
            assertThat(result).isEqualTo(expected);
        });
    }

    private void assertAllGroupAMetricsEqualTo(Float expectedFailureRate,
                                               Integer expectedSuccessCalls,
                                               Integer expectedBufferedCalls,
                                               Integer expectedMaxBufferedCalls,
                                               Integer expectedFailedCalls,
                                               Long expectedNotPermittedCalls) {
        assertCircuitBreakerMetricsEqualTo(circuitBreakersGroupA, expectedFailureRate, expectedSuccessCalls,
                expectedBufferedCalls, expectedMaxBufferedCalls, expectedFailedCalls, expectedNotPermittedCalls);
    }

    private void assertAllGroupBMetricsEqualTo(Float expectedFailureRate,
                                               Integer expectedSuccessCalls,
                                               Integer expectedBufferedCalls,
                                               Integer expectedMaxBufferedCalls,
                                               Integer expectedFailedCalls,
                                               Long expectedNotPermittedCalls) {
        assertCircuitBreakerMetricsEqualTo(circuitBreakersGroupB, expectedFailureRate, expectedSuccessCalls,
                expectedBufferedCalls, expectedMaxBufferedCalls, expectedFailedCalls, expectedNotPermittedCalls);
    }

    private void assertCircuitBreakerMetricsEqualTo(List<CircuitBreaker> circuitBreakers,
                                                    Float expectedFailureRate,
                                                    Integer expectedSuccessCalls,
                                                    Integer expectedBufferedCalls,
                                                    Integer expectedMaxBufferedCalls,
                                                    Integer expectedFailedCalls,
                                                    Long expectedNotPermittedCalls) {
        circuitBreakers.forEach(circuitBreaker -> {
            final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            if (expectedFailureRate != null) {
                assertThat(metrics.getFailureRate()).isEqualTo(expectedFailureRate);
            }
            if (expectedSuccessCalls != null) {
                assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(expectedSuccessCalls);
            }
            if (expectedBufferedCalls != null) {
                assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(expectedBufferedCalls);
            }
            if (expectedMaxBufferedCalls != null) {
                assertThat(metrics.getMaxNumberOfBufferedCalls()).isEqualTo(expectedMaxBufferedCalls);
            }
            if (expectedFailedCalls != null) {
                assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(expectedFailedCalls);
            }
            if (expectedNotPermittedCalls != null) {
                assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(expectedNotPermittedCalls);
            }
        });
    }

    private void assertThatAllGroupAMetricsAreReset() {
        this.assertAllGroupAMetricsEqualTo(-1f, 0, 0, null, 0, 0L);
    }
    private void assertThatAllGroupBMetricsAreReset() {
        this.assertAllGroupBMetricsEqualTo(-1f, 0, 0, null, 0, 0L);
    }

}
