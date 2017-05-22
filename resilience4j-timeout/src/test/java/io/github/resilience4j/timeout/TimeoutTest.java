package io.github.resilience4j.timeout;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;

import static io.github.resilience4j.timeout.SleepStubber.doSleep;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeoutTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofNanos(1);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SLEEP_DURATION = Duration.ofSeconds(1);

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
    public void decorateCheckedSupplier() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = Timeout.decorateCheckedSupplier(timeout, supplier);

        doSleep(SLEEP_DURATION).when(supplier).apply();

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.of(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.of(decorated);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = Timeout.decorateCheckedRunnable(timeout, runnable);

        doSleep(SLEEP_DURATION).when(runnable).run();
        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.run(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.run(decorated);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        CheckedFunction1<Integer, String> function = mock(CheckedFunction1.class);
        CheckedFunction1<Integer, String> decorated = Timeout.decorateCheckedFunction(timeout, function);

        doSleep(SLEEP_DURATION).when(function).apply(any());

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try<String> decoratedResult = Try.success(1).mapTry(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(1).mapTry(decorated);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateSupplier() throws Throwable {
        Supplier supplier = mock(Supplier.class);
        Supplier decorated = Timeout.decorateSupplier(timeout, supplier);

        doSleep(SLEEP_DURATION).when(supplier).get();

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.success(decorated).map(Supplier::get);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(decorated).map(Supplier::get);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateCallable() throws Throwable {
        Callable callable = mock(Callable.class);
        Callable decorated = Timeout.decorateCallable(timeout, callable);

        doSleep(SLEEP_DURATION).when(callable).call();

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.success(decorated).mapTry(Callable::call);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(decorated).mapTry(Callable::call);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateConsumer() throws Throwable {
        Consumer<Integer> consumer = mock(Consumer.class);
        Consumer<Integer> decorated = Timeout.decorateConsumer(timeout, consumer);

        doSleep(SLEEP_DURATION).when(consumer).accept(any());

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.success(1).andThen(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(1).andThen(decorated);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decoratedRunnable() throws Throwable {
        Runnable runnable = mock(Runnable.class);
        Runnable decorated = Timeout.decorateRunnable(timeout, runnable);

        doSleep(SLEEP_DURATION).when(runnable).run();
        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try decoratedResult = Try.success(decorated).andThen(Runnable::run);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(decorated).andThen(Runnable::run);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void decorateFunction() throws Throwable {
        Function<Integer, String> function = mock(Function.class);
        Function<Integer, String> decorated = Timeout.decorateFunction(timeout, function);

        doSleep(SLEEP_DURATION).when(function).apply(any());

        when(timeout.getTimeoutConfig())
                .thenReturn(shortConfig);

        Try<String> decoratedResult = Try.success(1).map(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause()).hasCause(JAVA_TIMEOUT_EXCEPTION);

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try secondResult = Try.success(1).map(decorated);
        then(secondResult.isSuccess()).isTrue();
    }

    @Test
    public void throwsExceptionWhenCheckedSupplierThrows() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = Timeout.decorateCheckedSupplier(timeout, supplier);

        doThrow(new IllegalStateException()).when(supplier).apply();

        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try decoratedResult = Try.of(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause().getCause()).isInstanceOf(ExecutionException.class);
        then(decoratedResult.getCause().getCause().getCause()).isInstanceOf(IllegalStateException.class);
    }


    @Test
    public void throwsExceptionWhenCheckedRunnableThrows() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = Timeout.decorateCheckedRunnable(timeout, runnable);

        doThrow(new IllegalStateException()).when(runnable).run();
        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try decoratedResult = Try.run(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause().getCause()).isInstanceOf(ExecutionException.class);
        then(decoratedResult.getCause().getCause().getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void throwsExceptionWhenCheckedFunctionThrows() throws Throwable {
        CheckedFunction1<Integer, String> function = mock(CheckedFunction1.class);
        CheckedFunction1<Integer, String> decorated = Timeout.decorateCheckedFunction(timeout, function);

        doThrow(new IllegalStateException()).when(function).apply(any());
        when(timeout.getTimeoutConfig())
                .thenReturn(longConfig);

        Try<String> decoratedResult = Try.success(1).mapTry(decorated);
        then(decoratedResult.isFailure()).isTrue();
        then(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        then(decoratedResult.getCause().getCause()).isInstanceOf(ExecutionException.class);
        then(decoratedResult.getCause().getCause().getCause()).isInstanceOf(IllegalStateException.class);
    }
}
