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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class CircuitBreakerAutoTransitionStateMachineTest {

    private CircuitBreaker circuitBreaker;
    private ScheduledExecutorService schedulerMock;

    @Before
    public void setUp() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindow(5, 5, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(2))
            .recordException(error -> !(error instanceof NumberFormatException))
            .build();

        SchedulerFactory schedulerFactoryMock = mock(SchedulerFactory.class);
        schedulerMock = mock(ScheduledExecutorService.class);
        when(schedulerFactoryMock.getScheduler()).thenReturn(schedulerMock);
        circuitBreaker = new CircuitBreakerStateMachine("testName", circuitBreakerConfig,
            schedulerFactoryMock);
    }

    @Test
    public void testAutoTransition() {
        // Initially the CircuitBreaker is open
        circuitBreaker.transitionToOpenState();

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delayArgumentCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitArgumentCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        // Check that schedule is invoked
        verify(schedulerMock)
            .schedule(runnableArgumentCaptor.capture(), delayArgumentCaptor.capture(),
                unitArgumentCaptor.capture());

        assertThat(delayArgumentCaptor.getValue()).isEqualTo(2000L);
        assertThat(unitArgumentCaptor.getValue()).isEqualTo(TimeUnit.MILLISECONDS);

        // Check that the runnable transitions to half_open
        runnableArgumentCaptor.getValue().run();

        assertThat(circuitBreaker.getState()).isEqualTo(HALF_OPEN);
    }

    @Test
    public void shouldCancelAutoTransition() {

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture)
            .when(schedulerMock).schedule(any(Runnable.class), any(Long.class), any(TimeUnit.class));

        // Auto transition scheduled
        circuitBreaker.transitionToOpenState();
        then(schedulerMock).should(times(1)).schedule(any(Runnable.class), any(Long.class), any(TimeUnit.class));

        // Auto transition should be canceled
        circuitBreaker.transitionToForcedOpenState();

        // Verify scheduled future is canceled
        then(mockFuture).should(times(1)).cancel(false);
    }

    @Test
    public void notCancelAutoTransitionFutureIfAlreadyDone() {

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture)
            .when(schedulerMock).schedule(any(Runnable.class), any(Long.class), any(TimeUnit.class));

        // Already done
        when(mockFuture.isDone()).thenReturn(true);

        // Auto transition scheduled
        circuitBreaker.transitionToOpenState();
        then(schedulerMock).should(times(1)).schedule(any(Runnable.class), any(Long.class), any(TimeUnit.class));

        // Auto transition should be canceled
        circuitBreaker.transitionToForcedOpenState();

        // Not called again because future is already done.
        then(mockFuture).should(times(0)).cancel(true);
    }
}
