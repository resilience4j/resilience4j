/*
 *
 *  Copyright 2020: KrnSaurabh
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
import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class VavrDecoratorsTest {
    private boolean state = false;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void testDecorateCheckedSupplierWithFallbackFromResult() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedFunction0<String> decoratedSupplier = VavrDecorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withFallback((result) -> result.equals("Hello world"), (result) -> "Bla")
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        String result = decoratedSupplier.apply();

        assertThat(result).isEqualTo("Bla");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void testDecorateCheckedSupplier() throws IOException {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedFunction0<String> decoratedSupplier = VavrDecorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = Try.of(decoratedSupplier).get();

        assertThat(result).isEqualTo("Hello world");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void testDecorateCheckedSupplierWithFallback() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        circuitBreaker.transitionToOpenState();

        CheckedFunction0<String> checkedSupplier = VavrDecorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withFallback(CallNotPermittedException.class, e -> "Fallback")
            .decorate();

        String result = checkedSupplier.apply();

        assertThat(result).isEqualTo("Fallback");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(1);
        then(helloWorldService).should(never()).returnHelloWorld();
    }

    @Test
    public void testDecorateCheckedRunnable() throws IOException {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedRunnable decoratedRunnable = VavrDecorators
            .ofCheckedRunnable(() -> helloWorldService.sayHelloWorldWithException())
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        Try.run(decoratedRunnable);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithException();
    }

    @Test
    public void testDecorateCheckedFunction() throws IOException {
        given(helloWorldService.returnHelloWorldWithNameWithException("Name"))
            .willReturn("Hello world Name");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");
        CheckedFunction1<String, String> decoratedFunction = VavrDecorators
            .ofCheckedFunction(helloWorldService::returnHelloWorldWithNameWithException)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate();

        String result = Try.of(() -> decoratedFunction.apply("Name")).get();

        assertThat(result).isEqualTo("Hello world Name");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
    }

    @Test
    public void testDecoratorBuilderWithRateLimiter() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .build();
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);
        CheckedFunction0<String> restrictedSupplier = VavrDecorators
            .ofCheckedSupplier(() -> helloWorldService.returnHelloWorld())
            .withRateLimiter(rateLimiter)
            .decorate();
        alignTime(rateLimiter);

        Try<String> firstTry = Try.of(restrictedSupplier);
        Try<String> secondTry = Try.of(restrictedSupplier);

        assertThat(firstTry.isSuccess()).isTrue();
        assertThat(secondTry.isFailure()).isTrue();
        assertThat(secondTry.getCause()).isInstanceOf(RequestNotPermitted.class);
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
    public void testDecorateCheckedSupplierWithCache() {
        javax.cache.Cache<String, String> cache = mock(javax.cache.Cache.class);
        given(cache.containsKey("testKey")).willReturn(true);
        given(cache.get("testKey")).willReturn("Hello from cache");
        CheckedFunction1<String, String> cachedFunction = VavrDecorators
            .ofCheckedSupplier(() -> "Hello world")
            .withCache(Cache.of(cache))
            .decorate();

        Try<String> value = Try.of(() -> cachedFunction.apply("testKey"));

        assertThat(value).contains("Hello from cache");
    }
}
