package io.github.resilience4j.timelimiter;

import io.vavr.control.Try;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

public class TimeLimiterTest {

    @Test
    public void shouldReturnCorrectTimeoutDuration() {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        assertThat(timeLimiter).isNotNull();
        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isEqualTo(timeoutDuration);
    }

    @Test
    public void shouldThrowTimeoutExceptionAndInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);
        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);

        then(mockFuture).should().cancel(true);
    }

    @Test
    public void shouldThrowTimeoutExceptionWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofMillis(300);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                // sleep for timeout.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 0;
        });

        CompletionStage<Integer> decorated = TimeLimiter
            .decorateCompletionStage(timeLimiter, scheduler, supplier).get();
        Try<Integer> decoratedResult = Try.ofCallable(() -> decorated.toCompletableFuture().get());
        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(TimeoutException.class);
    }

    @Test
    public void shouldThrowTimeoutExceptionAndNotInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter
            .of(TimeLimiterConfig.custom().timeoutDuration(timeoutDuration)
                .cancelRunningFuture(false).build());

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);
        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);

        then(mockFuture).should(never()).cancel(true);
    }

    @Test
    public void shouldReturnResult() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS)).willReturn(42);

        int result = timeLimiter.executeFutureSupplier(supplier);
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateFutureSupplier(supplier).call();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    public void shouldReturnResultWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                // sleep but not timeout.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 42;
        });

        int result = timeLimiter.executeCompletionStage(scheduler, supplier).toCompletableFuture()
            .get();
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateCompletionStage(scheduler, supplier).get()
            .toCompletableFuture().get();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    public void unwrapExecutionException() {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Supplier<Future<Integer>> supplier = () -> executorService.submit(() -> {
            throw new RuntimeException();
        });
        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.getCause() instanceof RuntimeException).isTrue();
    }
}
