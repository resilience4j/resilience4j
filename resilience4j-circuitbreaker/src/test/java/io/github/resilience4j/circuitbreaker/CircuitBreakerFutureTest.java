package io.github.resilience4j.circuitbreaker;

import com.jayway.awaitility.Duration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.CircuitBreakerFuture;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.is;

/**
 * Class CircuitBreakerFutureTest.
 */
public class CircuitBreakerFutureTest {
    private static ExecutorService executor = Executors.newCachedThreadPool();

    @AfterClass
    public static void cleanUp() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldDecorateFutureAndReturnSuccess() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> "Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);
        String value = decoratedFuture.get();

        assertThat(value).isEqualTo("Hello World");

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndCircuitBreakingLogicApplyOnceOnMultipleFutureEval() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> "Hello World");

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        //called twice but circuit breaking should be evaluated once.
        decoratedFuture.get();
        decoratedFuture.get();

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndThrowExecutionException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> {
            throw new RuntimeException("BAM!");
        });

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndThrowTimeoutException() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> {
            Thread.currentThread().sleep(10000);
            return null;
        });

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndCallerRequestCancelled() throws Exception{
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        //long running task
        final Future<String> future = executor.submit(() -> {Thread.sleep(10000); return null;});
        future.cancel(true);

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(CancellationException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndInterruptedExceptionThrownByTaskThread() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> {
            Thread.currentThread().interrupt();
            //Sleep should throw InterruptedException
            Thread.currentThread().sleep(10000);
            return null;
        });

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
    }

    @Test
    public void shouldDecorateFutureAndInterruptedExceptionThrownByCallingThread() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        //long running task
        final Future<String> future = executor.submit(() -> {
            Thread.currentThread().sleep(10000);
            return null;
        });

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture<>(circuitBreaker, future);

        AtomicBoolean isDone = new AtomicBoolean(false);
        AtomicReference reference = new AtomicReference(null);

        executor.submit(() -> {
            Thread.currentThread().interrupt();

            //should throw interrupt exception because get is a blocking operation
            reference.set(catchThrowable(() -> decoratedFuture.get()));
            isDone.set(true);
        });

        await().atMost(Duration.FIVE_SECONDS).untilAtomic(isDone, is(true));

        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        assertThat(reference.get()).isInstanceOf(InterruptedException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

}