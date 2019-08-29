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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class FluxCircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        circuitBreaker = Mockito.mock(CircuitBreaker.class);
    }

    @Test
    public void shouldSubscribeToFluxJust() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();

        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, times(1)).onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldPropagateErrorWhenErrorNotOnSubscribe() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
                Flux.error(new IOException("BAM!"), true)
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, times(1)).onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldSubscribeToMonoJustTwice(){
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(Flux.just("Event 1", "Event 2")
                .flatMap(value -> Mono.just("Bla " + value).compose(CircuitBreakerOperator.of(circuitBreaker))))
                .expectNext("Bla Event 1")
                .expectNext("Bla Event 2")
                .verifyComplete();

        verify(circuitBreaker, times(2)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldEmitErrorWithCircuitBreakerOpenException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitCircuitBreakerOpenExceptionEvenWhenErrorNotOnSubscribe() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
                Flux.error(new IOException("BAM!"), true)
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitCircuitBreakerOpenExceptionEvenWhenErrorDuringSubscribe() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
                Flux.just("Event")
                        .delayElements(Duration.ofDays(1))
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectSubscription()
                .thenCancel()
                .verify();

        verify(circuitBreaker, times(1)).releasePermission();
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldInvokeOnSuccessOnCancelWhenEventWasEmitted() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        StepVerifier.create(
                Flux.just("Event1", "Event2", "Event3")
                        .compose(CircuitBreakerOperator.of(circuitBreaker)))
                .expectSubscription()
                .thenRequest(1)
                .thenCancel()
                .verify();

        verify(circuitBreaker, never()).releasePermission();
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
    }
}