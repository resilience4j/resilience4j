package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.CircuitBreakerFuture;
import org.junit.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Class CircuitBreakerFutureTest.
 */
public class CircuitBreakerFutureTest {

    @Test
    public void shouldDecorateFutureAndReturnSuccess() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get()).thenReturn("Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);
        String value = decoratedFuture.get();

        assertThat(value).isEqualTo("Hello World");

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureAndCircuitBreakingLogicApplyOnceOnMultipleFutureEval() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get()).thenReturn("Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        //called twice but circuit breaking should be evaluated once.
        decoratedFuture.get();
        decoratedFuture.get();

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should(times(2)).get();
    }

    @Test
    public void shouldDecorateFutureAndThrowExecutionException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException("BAM!")));

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureAndThrowTimeoutException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new TimeoutException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldDecorateFutureAndCallerRequestCancelled() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new CancellationException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(CancellationException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureAndInterruptedExceptionThrownByTaskThread() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = mock(Future.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new ExecutionException(new InterruptedException()));

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        assertThat(thrown).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(InterruptedException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldDecorateFutureAndInterruptedExceptionThrownByCallingThread() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        //long running task
        final Future<String> future = mock(Future.class);
        when(future.get()).thenThrow(new InterruptedException());

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get());
        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        assertThat(thrown).isInstanceOf(InterruptedException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        then(future).should().get();
    }

}