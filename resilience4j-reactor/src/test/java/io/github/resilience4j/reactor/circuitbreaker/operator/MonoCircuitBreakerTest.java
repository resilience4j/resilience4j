/*
 * Copyright 2018 Julien Hoarau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class MonoCircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldSubscribeToMonoJust() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        StepVerifier.create(
            Mono.just(helloWorldService.returnHelloWorld())
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectNext("Hello World")
            .verifyComplete();

        then(helloWorldService).should().returnHelloWorld();
        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldSubscribeToMonoFromCallable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        StepVerifier.create(
            Mono.fromCallable(() -> helloWorldService.returnHelloWorld())
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectNext("Hello World")
            .verifyComplete();

        then(helloWorldService).should().returnHelloWorld();
        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldSubscribeToMonoFromCallableMultipleTimes() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        StepVerifier.create(
            Mono.fromCallable(() -> helloWorldService.returnHelloWorld())
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .repeat(2))
            .expectNext("Hello World")
            .expectNext("Hello World")
            .expectNext("Hello World")
            .verifyComplete();

        then(helloWorldService).should(times(3)).returnHelloWorld();
        verify(circuitBreaker, times(3)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void emptyMonoShouldBeSuccessful() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
            Mono.empty()
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .verifyComplete();

        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
            Mono.error(new IOException("BAM!"))
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectError(IOException.class)
            .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, times(1))
            .onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitCallNotPermittedExceptionEvenWhenErrorDuringSubscribe() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
            Mono.error(new IOException("BAM!"))
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectError(CallNotPermittedException.class)
            .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitErrorWithCircuitBreakerOpenException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
            Mono.just("Event")
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectError(CallNotPermittedException.class)
            .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
            Mono.just("Event")
                .delayElement(Duration.ofDays(1))
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectSubscription()
            .thenCancel()
            .verify();

        verify(circuitBreaker, times(1)).releasePermission();
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldNotSubscribeToMonoFromCallable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        StepVerifier.create(
            Mono.fromCallable(() -> helloWorldService.returnHelloWorld())
                .flatMap(Mono::just)
                .compose(CircuitBreakerOperator.of(circuitBreaker)))
            .expectError(CallNotPermittedException.class)
            .verify();

        then(helloWorldService).should(never()).returnHelloWorld();
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldRecordSuccessWhenUsingToFuture() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        try {
            Mono.just("Event")
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .toFuture()
                .get();

        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }
}