/*
 *
 *  Copyright 2026 Robert Winkler
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
package io.github.resilience4j.decorators;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

class DecoratorsTest {

    private boolean state = false;
    private HelloWorldService helloWorldService;
    private AsyncHelloWorldService asyncHelloWorldService;

    @BeforeEach
    void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        asyncHelloWorldService = mock(AsyncHelloWorldService.class);
    }

    @Test
    void shouldThrowTimeoutException() {
        TimeLimiter timeLimiter = TimeLimiter.of("helloBackend", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100)).build());
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        CompletionStage<String> completionStage = Decorators
            .ofCallable(() -> {
                Thread.sleep(1000);
                return "Bla";
            })
            .withThreadPoolBulkhead(bulkhead)
            .withTimeLimiter(timeLimiter, Executors.newSingleThreadScheduledExecutor())
            .withCircuitBreaker(circuitBreaker)
            .get();

        assertThatThrownBy(() -> completionStage.toCompletableFuture().get())
            .hasCauseInstanceOf(TimeoutException.class);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldThrowTimeoutExceptionAndPropagateContext() {
        TimeLimiter timeLimiter = TimeLimiter.of("helloBackend", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100)).build());
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        TestThreadLocalContextPropagatorWithHolder<String> propagator = new TestThreadLocalContextPropagatorWithHolder<>();
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        ContextAwareScheduledThreadPoolExecutor scheduledThreadPool = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(1)
            .contextPropagators(propagator)
            .build();

        CompletionStage<String> completionStage = Decorators
            .ofCallable(() -> {
                assertThat(Thread.currentThread().getName()).isEqualTo("bulkhead-helloBackend-1");
                Thread.sleep(1000);
                return "Bla";
            })
            .withThreadPoolBulkhead(bulkhead)
            .withTimeLimiter(timeLimiter, scheduledThreadPool)
            .withCircuitBreaker(circuitBreaker)
            .get();

        final CompletableFuture<String> completableFuture = completionStage.toCompletableFuture().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).isEqualTo("ContextAwareScheduledThreadPool-1");
                assertThat(TestThreadLocalContextHolder.get().get()).isEqualTo("ValueShouldCrossThreadBoundary");
                return (String) TestThreadLocalContextHolder.get().orElse(null);
            }
            return null;
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldThrowTimeoutExceptionAndPropagateMDCContext() {
        TimeLimiter timeLimiter = TimeLimiter.of("helloBackend", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100)).build());
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        MDC.put("key", "ValueShouldPropagateThreadBoundary");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        ContextAwareScheduledThreadPoolExecutor scheduledThreadPool = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(1)
            .build();

        CompletionStage<String> completionStage = Decorators
            .ofCallable(() -> {
                assertThat(Thread.currentThread().getName()).isEqualTo("bulkhead-helloBackend-1");
                Thread.sleep(1000);
                return "Bla";
            })
            .withThreadPoolBulkhead(bulkhead)
            .withTimeLimiter(timeLimiter, scheduledThreadPool)
            .withCircuitBreaker(circuitBreaker)
            .get();

        final CompletableFuture<String> completableFuture = completionStage.toCompletableFuture().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).isEqualTo("ContextAwareScheduledThreadPool-1");
                assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(contextMap);
                return MDC.getCopyOfContextMap().get("key");
            }
            return null;
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldPropagateThreadBoundary"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void decorateSupplier() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Supplier<String> decoratedSupplier = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void decorateSupplierWithFallbackFromResult() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Supplier<String> decoratedSupplier = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withFallback((result) -> result.equals("Hello world"), (result) -> "Bla")
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("Bla");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void decorateCallableWithFallbackFromResult() throws Exception {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Callable<String> decoratedSupplier = Decorators
            .ofCallable(() -> helloWorldService.returnHelloWorldWithException ())
            .withFallback((result) -> result.equals("Hello world"), (result) -> "Bla")
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        String result = decoratedSupplier.call();

        assertThat(result).isEqualTo("Bla");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void decorateFunctionWithFallbackFromResult() throws Exception {
        given(helloWorldService.returnHelloWorldWithName("Name")).willReturn("Hello world Name");
        Function<String, String> decoratedFunction = Decorators
                .ofFunction((Function<String, String>) str -> helloWorldService.returnHelloWorldWithName(str))
                .withFallback(result -> result.equals("Hello world Name"), (result) -> "Fallback")
                .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void decorateCheckedFunctionWithFallbackFromResult() throws Throwable {
        given(helloWorldService.returnHelloWorldWithName("Name")).willReturn("Hello world Name");
        CheckedFunction<String, String> decoratedFunction = Decorators
                .ofCheckedFunction((CheckedFunction<String, String>) str -> helloWorldService.returnHelloWorldWithName(str))
                .withFallback(result -> result.equals("Hello world Name"), (result) -> "Fallback")
                .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void decorateCheckedSupplierWithFallbackFromResult() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedSupplier<String> decoratedSupplier = Decorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withFallback((result) -> result.equals("Hello world"), (result) -> "Bla")
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("Bla");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void decorateCompletionStageWithFallbackFromResult() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> completionStage = Decorators
            .ofCompletionStage(completionStageSupplier)
            .withFallback((result) -> result.equals("Hello world"), (result) -> "Bla")
            .withCircuitBreaker(circuitBreaker)
            .get();

        String result = completionStage.toCompletableFuture().get();

        assertThat(result).isEqualTo("Bla");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void decorateSupplierWithThreadPoolBulkhead()
        throws Exception {

        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        CompletableFuture<String> future = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withThreadPoolBulkhead(ThreadPoolBulkhead.ofDefaults("helloBackend"))
            .withTimeLimiter(TimeLimiter.ofDefaults(), Executors.newSingleThreadScheduledExecutor())
            .withCircuitBreaker(circuitBreaker)
            .get().toCompletableFuture();

        String result = future.get();

        assertThat(result).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void decorateRunnableWithThreadPoolBulkhead()
        throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        CompletableFuture<Void> future = Decorators
            .ofRunnable(() -> helloWorldService.sayHelloWorld())
            .withThreadPoolBulkhead(ThreadPoolBulkhead.ofDefaults("helloBackend"))
            .withCircuitBreaker(circuitBreaker)
            .get().toCompletableFuture();

        future.get();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    void decorateCallable() throws Exception {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Callable<String> decoratedCallable = Decorators
            .ofCallable(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = decoratedCallable.call();

        assertThat(result).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void decorateSupplierWithFallback() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        circuitBreaker.transitionToOpenState();

        Supplier<String> decoratedSupplier = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withFallback(asList(IOException.class, CallNotPermittedException.class), (e) -> "Fallback")
            .decorate();

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();
        then(helloWorldService).should(never()).returnHelloWorld();
    }

    @Test
    void decorateCheckedSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedSupplier<String> decoratedSupplier = Decorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void decorateCheckedSupplierWithFallback() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        circuitBreaker.transitionToOpenState();

        CheckedSupplier<String> checkedSupplier = Decorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withFallback(CallNotPermittedException.class, e -> "Fallback")
            .decorate();

        String result = checkedSupplier.get();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();
        then(helloWorldService).should(never()).returnHelloWorld();
    }

    @Test
    void decorateCheckedSupplierWithThreadPoolBulkheadSucceeds() throws Exception {
        String expected = "Hello world";
        given(helloWorldService.returnHelloWorldWithException()).willReturn(expected);
        ThreadPoolBulkhead threadPoolBulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        CompletionStage<String> completionStage = Decorators
                .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .get();

        // make sure normal execution is successful
        String actual = completionStage.toCompletableFuture().get();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void decorateCheckedSupplierWithThreadPoolBulkheadWithExceptionThrown() throws Exception {
        IOException exception = new IOException("thrown from mock");
        given(helloWorldService.returnHelloWorldWithException()).willThrow(exception);
        ThreadPoolBulkhead threadPoolBulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        CompletionStage<String> completionStage = Decorators
                .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .get();

        assertThatThrownBy(() -> completionStage.toCompletableFuture().get())
                .hasCause(exception);
    }

    @Test
    @SuppressWarnings("unchecked")
    void decorateCheckedSupplierWithThreadPoolBulkheadFull() throws Exception {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        ThreadPoolBulkhead threadPoolBulkheadMock = mock(ThreadPoolBulkhead.class, RETURNS_DEEP_STUBS);

        willThrow(BulkheadFullException.createBulkheadFullException(threadPoolBulkheadMock)).given(threadPoolBulkheadMock).submit(isA(Callable.class));
        willDoNothing().given(helloWorldService).sayHelloWorldWithException();

        CompletionStage<String> completionStage = Decorators
                .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkheadMock)
                .get();
        assertThatThrownBy(() -> completionStage.toCompletableFuture().get())
                .hasCauseInstanceOf(BulkheadFullException.class);
    }

    @Test
    void decorateCallableWithFallback() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        circuitBreaker.transitionToOpenState();

        Callable<String> callable = Decorators
            .ofCallable(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withFallback(CallNotPermittedException.class, e -> "Fallback")
            .decorate();

        String result = callable.call();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();
        then(helloWorldService).should(never()).returnHelloWorld();
    }

    @Test
    @SuppressWarnings("unchecked")
    void decorateSupplierWithBulkheadFullExceptionFallback() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkheadMock = spy(bulkhead);

        given(bulkheadMock.submit(any(Callable.class))).willThrow(BulkheadFullException.createBulkheadFullException(bulkhead));

        CompletionStage<String> completionStage = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withThreadPoolBulkhead(bulkheadMock)
            .withFallback(BulkheadFullException.class, (e) -> "Fallback")
            .get();

        String result = completionStage.toCompletableFuture().get();

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    @SuppressWarnings("unchecked")
    void decorateCallableWithBulkheadFullExceptionFallback() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkheadMock = spy(bulkhead);
        given(bulkheadMock.submit(any(Callable.class))).willThrow(BulkheadFullException.createBulkheadFullException(bulkhead));

        CompletionStage<String> completionStage = Decorators
            .ofCallable(() -> helloWorldService.returnHelloWorldWithException())
            .withThreadPoolBulkhead(bulkheadMock)
            .withFallback(BulkheadFullException.class, (e) -> "Fallback")
            .get();

        String result = completionStage.toCompletableFuture().get();

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    @SuppressWarnings("unchecked")
    void decorateRunnableWithBulkheadFullExceptionFallback() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkheadMock = spy(bulkhead);
        given(bulkheadMock.submit(any(Callable.class))).willThrow(BulkheadFullException.createBulkheadFullException(bulkhead));

        CompletionStage<Void> completionStage = Decorators
            .ofRunnable(() -> helloWorldService.sayHelloWorld())
            .withThreadPoolBulkhead(bulkheadMock)
            .withFallback(BulkheadFullException.class, (e) -> {
                helloWorldService.sayHelloWorld();
                return null;
            })
            .get();

        completionStage.toCompletableFuture().get();

        then(helloWorldService).should(times(1)).sayHelloWorld();
    }


    @Test
    void decorateCompletionStageWithCallNotPermittedExceptionFallback() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        circuitBreaker.transitionToOpenState();
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        CompletionStage<String> completionStage = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withThreadPoolBulkhead(bulkhead)
            .withCircuitBreaker(circuitBreaker)
            .withFallback(CallNotPermittedException.class, (e) -> "Fallback")
            .get();

        String result = completionStage.toCompletableFuture().get();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();
    }

    @Test
    void decorateCompletionStageWithTimeoutExceptionFallback() throws Exception {
        TimeLimiter timeLimiter = TimeLimiter.of("helloBackend", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100)).build());
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");
        CompletionStage<String> completionStage = Decorators
            .ofCallable(() -> {
                Thread.sleep(1000);
                return "Bla";
            })
            .withThreadPoolBulkhead(bulkhead)
            .withTimeLimiter(timeLimiter, Executors.newSingleThreadScheduledExecutor())
            .withCircuitBreaker(circuitBreaker)
            .withFallback(TimeoutException.class, (e) -> "Fallback")
            .get();

        String result = completionStage.toCompletableFuture().get();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void decorateRunnable() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Runnable decoratedRunnable = Decorators
            .ofRunnable(() -> helloWorldService.sayHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        decoratedRunnable.run();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }


    @Test
    void decorateCheckedRunnable() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedRunnable decoratedRunnable = Decorators
            .ofCheckedRunnable(() -> helloWorldService.sayHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        decoratedRunnable.run();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).sayHelloWorldWithException();
    }

    @Test
    void decorateCheckedRunnableWithThreadPoolBulkheadSucceeds() throws Exception {
        willDoNothing().given(helloWorldService).sayHelloWorldWithException();
        ThreadPoolBulkhead threadPoolBulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        CompletionStage<Void> completionStage = Decorators
                .ofCheckedRunnable(() -> helloWorldService.sayHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .get();

        // make sure normal execution is successful
        completionStage.toCompletableFuture().get();
    }

    @Test
    void decorateCheckedRunnableWithThreadPoolBulkheadWithExceptionThrown() throws Exception {
        IOException exception = new IOException("thrown from mock");
        willThrow(exception).given(helloWorldService).sayHelloWorldWithException();
        ThreadPoolBulkhead threadPoolBulkhead = ThreadPoolBulkhead.ofDefaults("helloBackend");

        CompletionStage<Void> completionStage = Decorators
                .ofCheckedRunnable(() -> helloWorldService.sayHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .get();

        assertThatThrownBy(() -> completionStage.toCompletableFuture().get())
                .hasCause(exception);
    }

    @Test
    void decorateCheckedRunnableWithThreadPoolBulkheadFull() throws Exception {
        ThreadPoolBulkhead threadPoolBulkheadMock = mock(ThreadPoolBulkhead.class, RETURNS_DEEP_STUBS);

        willThrow(BulkheadFullException.createBulkheadFullException(threadPoolBulkheadMock)).given(threadPoolBulkheadMock).submit(any(Runnable.class));
        willDoNothing().given(helloWorldService).sayHelloWorldWithException();

        CompletionStage<Void> completionStage = Decorators
                .ofCheckedRunnable(() -> helloWorldService.sayHelloWorldWithException())
                .withThreadPoolBulkhead(threadPoolBulkheadMock)
                .get();
        assertThatThrownBy(() -> completionStage.toCompletableFuture().get())
                .hasCauseInstanceOf(BulkheadFullException.class);
    }

    @Test
    void decorateCompletionStage() throws Exception {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> completionStage = Decorators
            .ofCompletionStage(completionStageSupplier)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"), Executors.newSingleThreadScheduledExecutor())
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .get();

        String value = completionStage.toCompletableFuture().get();

        assertThat(value).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void decorateCompletionStagePropagatesContextWithRetryAsync() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        TestThreadLocalContextPropagatorWithHolder<String> propagator = new TestThreadLocalContextPropagatorWithHolder<>();
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        ContextAwareScheduledThreadPoolExecutor scheduledThreadPool = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(1)
            .contextPropagators(propagator)
            .build();

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new HelloWorldException());
        given(asyncHelloWorldService.returnHelloWorld())
            .willReturn(failedFuture);

        CompletionStage<String> completionStage = Decorators
            .ofCompletionStage(() -> asyncHelloWorldService.returnHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"), scheduledThreadPool)
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .get();

        final CompletableFuture<String> completableFuture = completionStage.toCompletableFuture().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool");
                assertThat(TestThreadLocalContextHolder.get().get()).isEqualTo("ValueShouldCrossThreadBoundary");
                return (String) TestThreadLocalContextHolder.get().orElse(null);
            }
            return null;
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        then(asyncHelloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void decorateCompletionStagePropagatesMDCContextWithRetryAsync() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        MDC.put("key", "ValueShouldCrossThreadBoundary");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        ContextAwareScheduledThreadPoolExecutor scheduledThreadPool = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(1)
            .build();

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new HelloWorldException());
        given(asyncHelloWorldService.returnHelloWorld())
            .willReturn(failedFuture);

        CompletionStage<String> completionStage = Decorators
            .ofCompletionStage(() -> asyncHelloWorldService.returnHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"), scheduledThreadPool)
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .get();

        final CompletableFuture<String> completableFuture = completionStage.toCompletableFuture().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool");
                assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(contextMap);
                return MDC.getCopyOfContextMap().get("key");
            }
            return null;
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        then(asyncHelloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void executeConsumer() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Decorators.DecorateConsumer<String> decoratedConsumer =
            Decorators.ofConsumer((String input) -> helloWorldService
                .sayHelloWorldWithName(input))
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(Bulkhead.ofDefaults("testName"))
                .withRateLimiter(RateLimiter.ofDefaults("testName"));

        decoratedConsumer.accept("test");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).sayHelloWorldWithName("test");
    }

    @Test
    void decorateFunction() {
        given(helloWorldService.returnHelloWorldWithName("Name")).willReturn("Hello world Name");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Function<String, String> decoratedFunction = Decorators
            .ofFunction(helloWorldService::returnHelloWorldWithName)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Hello world Name");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
    }

    @Test
    void decorateFunctionWithFallback() {
        given(helloWorldService.returnHelloWorldWithName("Name")).willThrow(new RuntimeException("BAM!"));
        Function<String, String> decoratedFunction = Decorators
                .ofFunction(helloWorldService::returnHelloWorldWithName)
                .withFallback(throwable -> "Fallback")
                .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void decorateCheckedFunction() throws Throwable {
        given(helloWorldService.returnHelloWorldWithNameWithException("Name"))
            .willReturn("Hello world Name");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedFunction<String, String> decoratedFunction = Decorators
            .ofCheckedFunction(helloWorldService::returnHelloWorldWithNameWithException)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Hello world Name");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
    }

    @Test
    void decorateCheckedFunctionWithFallback() throws Throwable {
        given(helloWorldService.returnHelloWorldWithName("Name")).willThrow(new RuntimeException("BAM!"));
        CheckedFunction<String, String> decoratedFunction = Decorators
                .ofCheckedFunction(helloWorldService::returnHelloWorldWithName)
                .withFallback(throwable -> "Fallback")
                .decorate();

        String result = decoratedFunction.apply("Name");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void decorateCheckedConsumer() throws Throwable {
        given(helloWorldService.returnHelloWorldWithName("Name"))
            .willReturn("Hello world Name");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedConsumer<String> decoratedConsumer = Decorators
            .ofCheckedConsumer(helloWorldService::returnHelloWorldWithName)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        decoratedConsumer.accept("Name");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldWithName("Name");
    }

    @Test
    void decoratorBuilderWithRetry() {
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        Supplier<String> decoratedSupplier = Decorators
            .ofSupplier(() -> helloWorldService.returnHelloWorld())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        assertThatThrownBy(decoratedSupplier::get).isInstanceOf(RuntimeException.class);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void decoratorBuilderWithRateLimiter() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .build();
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);
        CheckedSupplier<String> restrictedSupplier = Decorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorld())
            .withRateLimiter(rateLimiter)
            .decorate();
        alignTime(rateLimiter);

        String firstResult = restrictedSupplier.get();
        assertThatThrownBy(restrictedSupplier::get).isInstanceOf(RequestNotPermitted.class);

        assertThat(firstResult).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    private void alignTime(RateLimiter rateLimiter) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        while (rateLimiter.acquirePermission()) {
            state = !state;
        }
        // Wait to the start of the next cycle in spin loop
        while (metrics.getAvailablePermissions() == 0) {
            state = !state;
        }
        System.out.println(state);
    }

    @SuppressWarnings("unchecked")
    @Test
    void decorateCheckedSupplierWithCache() throws Throwable {
        javax.cache.Cache<String, String> cache = mock(javax.cache.Cache.class);
        given(cache.containsKey("testKey")).willReturn(true);
        given(cache.get("testKey")).willReturn("Hello from cache");
        CheckedFunction<String, String> cachedFunction = Decorators
            .ofCheckedSupplier(() -> "Hello world")
            .withCache(Cache.of(cache))
            .decorate();

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello from cache");
    }


    @SuppressWarnings("unchecked")
    @Test
    void decorateSupplierWithCache() {
        javax.cache.Cache<String, String> cache = mock(javax.cache.Cache.class);
        given(cache.containsKey("testKey")).willReturn(true);
        given(cache.get("testKey")).willReturn("Hello from cache");
        Function<String, String> cachedFunction = Decorators
            .ofSupplier(() -> "Hello world")
            .withCache(Cache.of(cache))
            .decorate();

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello from cache");
    }

}
