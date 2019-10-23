package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.CircuitBreakerFuture;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture(circuitBreaker, future);
        String value = decoratedFuture.get();

        assertThat(value).isEqualTo("Hello World");

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

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture(circuitBreaker, future);

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

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndCancelled() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        final Future<String> future = executor.submit(() -> "Hello World");
        future.cancel(true);

        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture(circuitBreaker, future);
        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(CancellationException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldDecorateFutureAndPermissionNotPermited() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        circuitBreaker.transitionToOpenState();

        final Future<String> future = executor.submit(() -> "Hello World");
        CircuitBreakerFuture<String> decoratedFuture = new CircuitBreakerFuture(circuitBreaker, future);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
