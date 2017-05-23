package io.github.resilience4j.timeout;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import io.vavr.control.Try;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeoutTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofNanos(1);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SLEEP_DURATION = Duration.ofSeconds(5);

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private static final java.util.concurrent.TimeoutException JAVA_TIMEOUT_EXCEPTION = new java.util.concurrent.TimeoutException();

    private TimeoutConfig shortConfig;
    private TimeoutConfig longConfig;
    private Timeout timeout;

    @Before
    public void init() {
        shortConfig = TimeoutConfig.custom()
                .timeoutDuration(SHORT_TIMEOUT)
                .build();
        longConfig = TimeoutConfig.custom()
                .timeoutDuration(LONG_TIMEOUT)
                .build();
        timeout = mock(Timeout.class);
    }

    @Test
    public void construction() throws Exception {
        Timeout timeout = Timeout.of(shortConfig);
        then(timeout).isNotNull();
    }

    @Test
    public void decorateFuture() throws Throwable {
        when(timeout.getTimeoutConfig()).thenReturn(shortConfig);

        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
            );

        Future<Integer> decorated = Timeout.decorateFuture(timeout, future);

        Try decoratedResult = Try.success(decorated).mapTry(Future::get);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        decorated = Timeout.decorateFuture(timeout, future);

        Try secondResult = Try.success(decorated).mapTry(Future::get);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void executeFuture() throws Throwable {
        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Try decoratedResult = Try.of(() -> Timeout.of(shortConfig).executeFuture(future));
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        Try secondResult = Try.of(() -> Timeout.of(longConfig).executeFuture(future));
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateFutureSupplier() throws Throwable {
        when(timeout.getTimeoutConfig()).thenReturn(shortConfig);

        Future<Integer> future = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                    return 1;
                }
        );

        Supplier<Future<Integer>> supplier = () -> future;
        Supplier<Future<Integer>> decorated = Timeout.decorateFutureSupplier(timeout, supplier);

        Try decoratedResult = Try.success(decorated).mapTry(Supplier::get).mapTry(Future::get);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        decorated = Timeout.decorateFutureSupplier(timeout, supplier);

        Try secondResult = Try.success(decorated).mapTry(Supplier::get).mapTry(Future::get);
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

        Try decoratedResult = Try.of(() -> Timeout.of(shortConfig).executeFutureSupplier(supplier)).mapTry(Future::get);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        Try secondResult = Try.of(() -> Timeout.of(longConfig).executeFutureSupplier(supplier)).mapTry(Future::get);
        then(secondResult.isSuccess()).isTrue();
    }
}
