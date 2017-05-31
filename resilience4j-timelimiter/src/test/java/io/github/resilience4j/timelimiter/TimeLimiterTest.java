package io.github.resilience4j.timelimiter;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

import io.vavr.control.Try;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeLimiterTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofNanos(1);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SLEEP_DURATION = Duration.ofSeconds(5);

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private TimeLimiterConfig shortConfig;
    private TimeLimiterConfig longConfig;
    private TimeLimiter timeLimiter;

    @Before
    public void init() {
        shortConfig = TimeLimiterConfig.custom()
                .timeoutDuration(SHORT_TIMEOUT)
                .build();
        longConfig = TimeLimiterConfig.custom()
                .timeoutDuration(LONG_TIMEOUT)
                .build();
        timeLimiter = mock(TimeLimiter.class);
    }

    @Test
    public void construction() throws Exception {
        TimeLimiter timeLimiter = TimeLimiter.of(shortConfig);
        then(timeLimiter).isNotNull();
    }

    @Test
    public void defaultConstruction() throws Exception {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults();
        then(timeLimiter).isNotNull();
    }

    @Test
    public void durationConstruction() throws Exception {
        TimeLimiter timeLimiter = TimeLimiter.of(SHORT_TIMEOUT);
        then(timeLimiter).isNotNull();
        then(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(SHORT_TIMEOUT);
    }

    @Test
    public void decorateFuture() throws Throwable {
        when(timeLimiter.getTimeLimiterConfig()).thenReturn(shortConfig);

        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
            );

        Callable<Integer> decorated = TimeLimiter.decorateFuture(timeLimiter, future);

        Try decoratedResult = Try.success(decorated).mapTry(Callable::call);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(future.isCancelled()).isTrue();

        when(timeLimiter.getTimeLimiterConfig())
                .thenReturn(longConfig);

        future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        decorated = TimeLimiter.decorateFuture(timeLimiter, future);

        Try secondResult = Try.success(decorated).mapTry(Callable::call);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void executeFuture() throws Throwable {
        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Try decoratedResult = Try.of(() -> TimeLimiter.of(shortConfig).executeFuture(future));
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(future.isCancelled()).isTrue();

        Future<Integer> secondFuture = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Try secondResult = Try.of(() -> TimeLimiter.of(longConfig).executeFuture(secondFuture));
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateFutureSupplier() throws Throwable {
        when(timeLimiter.getTimeLimiterConfig()).thenReturn(shortConfig);

        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Supplier<Future<Integer>> supplier = () -> future;
        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        Try decoratedResult = Try.success(decorated).mapTry(Callable::call);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(future.isCancelled()).isTrue();

        when(timeLimiter.getTimeLimiterConfig())
                .thenReturn(longConfig);

        Future<Integer> secondFuture = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        supplier = () -> secondFuture;

        decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        Try secondResult = Try.success(decorated).mapTry(Callable::call);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void executeFutureSupplier() throws Throwable {
        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Supplier<Future<Integer>> supplier = () -> future;

        Try decoratedResult = Try.of(() -> TimeLimiter.of(shortConfig).executeFutureSupplier(supplier));
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(future.isCancelled()).isTrue();

        Future<Integer> secondFuture = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Supplier<Future<Integer>> secondSupplier = () -> secondFuture;

        Try secondResult = Try.of(() -> TimeLimiter.of(longConfig).executeFutureSupplier(secondSupplier));
        then(secondResult.isSuccess()).isTrue();
    }
}
