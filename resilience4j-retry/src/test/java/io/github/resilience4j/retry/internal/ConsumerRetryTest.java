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
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

public class ConsumerRetryTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldNotRetry() {
        Retry retryContext = Retry.ofDefaults("id");
        Consumer<String> consumer = Retry.decorateConsumer(retryContext, helloWorldService::sayHelloWorldWithName);

        consumer.accept("Name");

        then(helloWorldService).should().sayHelloWorldWithName("Name");
        assertThat(sleptTime).isZero();
    }

    @Test
    public void testDecorateConsumer() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        Retry retry = Retry.ofDefaults("id");
        Consumer<String> consumer = Retry.decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        assertThatThrownBy(() -> consumer.accept("Name"))
            .isInstanceOf(HelloWorldException.class);

        then(helloWorldService).should(times(3)).sayHelloWorldWithName("Name");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void testDecorateConsumerAndInvokeTwice() {
        doThrow(new HelloWorldException())
                .doNothing()
                .doThrow(new HelloWorldException())
                .doNothing()
                .when(helloWorldService).sayHelloWorldWithName("Name");

        Retry retry = Retry.ofDefaults("id");
        Consumer<String> consumer = Retry
                .decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        consumer.accept("Name");
        consumer.accept("Name");

        then(helloWorldService).should(times(4)).sayHelloWorldWithName("Name");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(2);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() throws Throwable {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithNameWithException("Name");
        Retry retry = Retry.ofDefaults("id");
        CheckedConsumer<String> retryableConsumer = Retry
            .decorateCheckedConsumer(retry, helloWorldService::sayHelloWorldWithNameWithException);

        assertThatThrownBy(() -> retryableConsumer.accept("Name"))
            .isInstanceOf(HelloWorldException.class);

        then(helloWorldService).should(times(3)).sayHelloWorldWithNameWithException("Name");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldReturnAfterOneAttempt() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
        Retry retry = Retry.of("id", config);
        CheckedConsumer<String> retryableConsumer = Retry
            .decorateCheckedConsumer(retry, helloWorldService::sayHelloWorldWithName);

        assertThatThrownBy(() -> retryableConsumer.accept("Name"))
            .isInstanceOf(HelloWorldException.class);

        then(helloWorldService).should().sayHelloWorldWithName("Name");
        assertThat(sleptTime).isZero();
    }

    @Test
    public void shouldReturnAfterOneAttemptAndIgnoreException() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        RetryConfig config = RetryConfig.custom()
            .retryOnException(throwable -> !(throwable instanceof HelloWorldException))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedConsumer<String> retryableConsumer = Retry
            .decorateCheckedConsumer(retry, helloWorldService::sayHelloWorldWithName);

        assertThatThrownBy(() -> retryableConsumer.accept("Name"))
            .isInstanceOf(HelloWorldException.class);

        // because the exception should be rethrown immediately
        then(helloWorldService).should().sayHelloWorldWithName("Name");
        assertThat(sleptTime).isZero();
    }

    @Test
    public void shouldTakeIntoAccountBackoffFunction() throws Throwable {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        RetryConfig config = RetryConfig
            .custom()
            .intervalFunction(IntervalFunction.of(Duration.ofMillis(500), x -> x * x))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedConsumer<String> retryableConsumer = Retry
            .decorateCheckedConsumer(retry, helloWorldService::sayHelloWorldWithName);

        try {
            retryableConsumer.accept("Name");
        } catch (HelloWorldException ignored) {
        }

        then(helloWorldService).should(times(3)).sayHelloWorldWithName("Name");
        assertThat(sleptTime).isEqualTo(
            RetryConfig.DEFAULT_WAIT_DURATION +
                RetryConfig.DEFAULT_WAIT_DURATION * RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldTakeIntoAccountRetryOnResult() throws Throwable {
        AtomicInteger value = new AtomicInteger(0);
        final int targetValue = 2;
        RetryConfig config = RetryConfig
                .custom()
                .retryOnResult(result -> value.get() != targetValue)
                .build();
        Retry retry = Retry.of("shouldTakeIntoAccountRetryOnResult", config);
        CheckedConsumer<String> retryableConsumer = Retry
                .decorateCheckedConsumer(retry, (name) -> {
                    helloWorldService.sayHelloWorldWithName(name);
                    value.incrementAndGet();
                });

        retryableConsumer.accept("Name");

        then(helloWorldService).should(times(targetValue)).sayHelloWorldWithName("Name");
        System.out.println(sleptTime);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldReturnAfterThreeAttemptsAndRecover() throws Throwable {

        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        doNothing().when(helloWorldService).sayHelloWorldWithName("RecoverName");

        Retry retry = Retry.ofDefaults("id");
        Consumer<String> retryableConsumer = Retry
                .decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        try {
            retryableConsumer.accept("Name");
        } catch (HelloWorldException e) {
            retryableConsumer.accept("RecoverName");
        }
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);

        then(helloWorldService).should(times(3)).sayHelloWorldWithName("Name");
        then(helloWorldService).should(times(1)).sayHelloWorldWithName("RecoverName");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldMarkThreadInterruptedWhenInterruptedDuringRetry() {
        RetryImpl.sleepFunction = sleep -> {
            throw new InterruptedException("Interrpted!");
        };
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");
        Retry retry = Retry.ofDefaults("id");
        Consumer<String> retryableConsumer = Retry
                .decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        try {
            retryableConsumer.accept("Name");
        } catch (Exception e) {
            retryableConsumer.accept("RecoverName");
        }
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    public void shouldThrowMaxRetriesExceededIfConfigured() {
        doNothing().when(helloWorldService).sayHelloWorldWithName("Name");

        RetryConfig retryConfig = RetryConfig.custom()
                .retryOnResult(s -> true)
                .maxAttempts(3)
                .failAfterMaxAttempts(true)
                .build();
        Retry retry = Retry.of("test", retryConfig);
        Consumer<String> consumer = Retry
                .decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        assertThatThrownBy(() -> consumer.accept("Name"))
                .isInstanceOf(MaxRetriesExceededException.class)
                .hasMessage("Retry 'test' has exhausted all attempts (3)");

        then(helloWorldService).should(times(3)).sayHelloWorldWithName("Name");
    }

    @Test
    public void shouldNotThrowMaxRetriesExceededIfCompletedExceptionally() {

        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorldWithName("Name");

        RetryConfig retryConfig = RetryConfig.<String>custom()
                .retryOnException(HelloWorldException.class::isInstance)
                .maxAttempts(3)
                .failAfterMaxAttempts(true)
                .build();
        Retry retry = Retry.of("test", retryConfig);
        Consumer<String> consumer = Retry
                .decorateConsumer(retry, helloWorldService::sayHelloWorldWithName);

        assertThatThrownBy(() -> consumer.accept("Name"))
                .isInstanceOf(HelloWorldException.class)
                .hasMessage("BAM!");

        then(helloWorldService).should(times(3)).sayHelloWorldWithName("Name");
    }
}
