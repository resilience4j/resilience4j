package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CallableTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldPropagateLastExceptionWhenSleepFunctionThrowsException() throws IOException {
        willThrow(new HelloWorldException()).given(helloWorldService).returnHelloWorldWithException();
        RetryConfig config = RetryConfig.custom()
            .intervalFunction((a) -> -1L)
            .build();
        Retry retry = Retry.of("id", config);
        Callable<String> retryableCallable = Retry
            .decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        Try<Void> result = Try.run(retryableCallable::call);

        then(helloWorldService).should().returnHelloWorldWithException();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
    }

    @Test
    public void shouldStopRetryingAndEmitProperEventsIfIntervalFunctionReturnsLessThanZero() throws IOException {
        given(helloWorldService.returnHelloWorldWithException())
                .willThrow(new HelloWorldException("Exceptional!"));

        AtomicInteger numberOfTimesIntervalFunctionCalled = new AtomicInteger(0);
        RetryConfig retryConfig = RetryConfig.<String>custom()
                .intervalFunction((ignored) -> {
                    int numTimesCalled = numberOfTimesIntervalFunctionCalled.incrementAndGet();
                    return numTimesCalled > 1 ? -1L : 0L;
                })
                .maxAttempts(3)
                .build();

        AtomicInteger numberOfRetryEvents = new AtomicInteger();
        AtomicBoolean onErrorEventOccurred = new AtomicBoolean(false);

        Retry retry = Retry.of("retry", retryConfig);
        retry.getEventPublisher().onRetry((ignored) -> numberOfRetryEvents.getAndIncrement());
        retry.getEventPublisher().onError((ignored) -> onErrorEventOccurred.set(true));

        Callable<String> callable = Retry.decorateCallable(
                retry,
                helloWorldService::returnHelloWorldWithException
        );

        Try<Void> result = Try.run(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get())
                .isInstanceOf(HelloWorldException.class);
        assertThat(numberOfRetryEvents).hasValue(1);
        assertThat(onErrorEventOccurred).isTrue();
        then(helloWorldService).should(times(2)).returnHelloWorldWithException();
    }

    @Test
    public void shouldContinueRetryingAndEmitProperEventsIfIntervalFunctionReturnsZeroOrMore() throws IOException {
        given(helloWorldService.returnHelloWorldWithException())
                .willThrow(new HelloWorldException("Exceptional!"));

        AtomicInteger numberOfTimesIntervalFunctionCalled = new AtomicInteger(0);
        RetryConfig retryConfig = RetryConfig.<String>custom()
                .intervalFunction((ignored) -> {
                    // Returns 0, 1, 2
                    return (long) numberOfTimesIntervalFunctionCalled.getAndIncrement();
                })
                .maxAttempts(3)
                .build();

        AtomicInteger numberOfRetryEvents = new AtomicInteger();
        AtomicBoolean onErrorEventOccurred = new AtomicBoolean(false);

        Retry retry = Retry.of("retry", retryConfig);
        retry.getEventPublisher().onRetry((ignored) -> numberOfRetryEvents.getAndIncrement());
        retry.getEventPublisher().onError((ignored) -> onErrorEventOccurred.set(true));

        Callable<String> callable = Retry.decorateCallable(
                retry,
                helloWorldService::returnHelloWorldWithException
        );

        Try<Void> result = Try.run(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get())
                .isInstanceOf(HelloWorldException.class);
        assertThat(numberOfRetryEvents).hasValue(2);
        assertThat(onErrorEventOccurred).isTrue();
        then(helloWorldService).should(times(3)).returnHelloWorldWithException();
    }


}
