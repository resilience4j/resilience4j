/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.CircuitBreakerFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Class CircuitBreakerFutureTest.
 */
class CircuitBreakerFutureTest {

    @Test
    void shouldDecorateFutureAndReturnSuccess() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get()).thenReturn("Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);
        String value = decoratedFuture.get();

        assertThat(value).isEqualTo("Hello World");

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureAndCircuitBreakingLogicApplyOnceOnMultipleFutureEval()
        throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get()).thenReturn("Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);

        //called twice but circuit breaking should be evaluated once.
        decoratedFuture.get();
        decoratedFuture.get();

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should(times(2)).get();
    }

    @Test
    void shouldDecorateFutureAndThrowExecutionException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException("BAM!")));

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureAndThrowTimeoutException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new TimeoutException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldDecorateFutureAndCallerRequestCancelled() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new CancellationException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(CancellationException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureAndInterruptedExceptionThrownByTaskThread() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get(anyLong(), any(TimeUnit.class)))
            .thenThrow(new ExecutionException(new InterruptedException()));

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        assertThat(thrown).isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(InterruptedException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldDecorateFutureAndInterruptedExceptionThrownByCallingThread()
        throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        //long running task
        @SuppressWarnings("unchecked")
        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new InterruptedException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker,
            future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get());
        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        assertThat(thrown).isInstanceOf(InterruptedException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

}
