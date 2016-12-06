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
package io.github.robwin.decorators;

import io.github.robwin.cache.Cache;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.ratelimiter.RateLimiter;
import io.github.robwin.ratelimiter.RateLimiterConfig;
import io.github.robwin.ratelimiter.RequestNotPermitted;
import io.github.robwin.retry.Retry;
import io.github.robwin.test.HelloWorldService;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class DecoratorsTest {

    private static final Logger LOG = LoggerFactory.getLogger(DecoratorsTest.class);

    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void testDecoratorBuilder(){
        // Given the HelloWorldService returns Hello world
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        Supplier<String> decoratedSupplier = Decorators.ofSupplier(() -> helloWorldService.returnHelloWorld())
                .withCircuitBreaker(circuitBreaker)
                .withRetry(Retry.ofDefaults())
                .decorate();

        String result = decoratedSupplier.get();
        assertThat(result).isEqualTo("Hello world");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();

    }

    @Test
    public void testDecoratorBuilderWithRetry(){
        // Given the HelloWorldService returns Hello world
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));;
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("helloBackend");

        Supplier<String> decoratedSupplier = Decorators.ofSupplier(() -> helloWorldService.returnHelloWorld())
                .withCircuitBreaker(circuitBreaker)
                .withRetry(Retry.ofDefaults())
                .decorate();

        Try.of(decoratedSupplier::get);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void testDecoratorBuilderWithRateLimiter(){
        // Given the HelloWorldService returns Hello world
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // Create a custom RateLimiter configuration
        RateLimiterConfig config = RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(100))
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .build();

        // Create a RateLimiter
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);

        Try.CheckedSupplier<String> restrictedSupplier = Decorators.ofCheckedSupplier(() -> helloWorldService.returnHelloWorld())
                .withRateLimiter(rateLimiter)
                .decorate();

        Try<String> firstTry = Try.of(restrictedSupplier);
        assertThat(firstTry.isSuccess()).isTrue();
        Try<String> secondTry = Try.of(restrictedSupplier);
        assertThat(secondTry.isFailure()).isTrue();
        assertThat(secondTry.getCause()).isInstanceOf(RequestNotPermitted.class);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void testDecoratorBuilderWitCache(){
        javax.cache.Cache<String, String> cache = mock(javax.cache.Cache.class);
        // Given the cache contains the key
        given(cache.containsKey("testKey")).willReturn(true);
        // Return the value from cache
        given(cache.get("testKey")).willReturn("Hello from cache");

        Try.CheckedFunction<String, String> cachedFunction = Decorators.ofCheckedSupplier(() -> "Hello world")
            .withCache(Cache.of(cache))
            .decorate();
        String value = Try.of(() -> cachedFunction.apply("testKey")).get();
        assertThat(value).isEqualTo("Hello from cache");
    }

}
