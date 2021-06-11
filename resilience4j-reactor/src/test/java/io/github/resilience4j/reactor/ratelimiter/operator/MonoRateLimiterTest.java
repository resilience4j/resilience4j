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
package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.ResponseWithPotentialOverload.SpecificResponseWithPotentialOverload;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static io.github.resilience4j.core.ResultUtils.isFailedAndThrown;
import static io.github.resilience4j.core.ResultUtils.isSuccessfulAndReturned;
import static io.github.resilience4j.reactor.ratelimiter.operator.OverloadException.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class MonoRateLimiterTest {

    @Test
    public void shouldEmitEvent() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        StepVerifier.create(
            Mono.just("Event")
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectNext("Event")
            .verifyComplete();
    }

    @Test
    public void shouldPropagateError() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        StepVerifier.create(
            Mono.error(new IOException("BAM!"))
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectError(IOException.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldDelaySubscription() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofMillis(50).toNanos());

        StepVerifier.create(
            Mono.error(new IOException("BAM!"))
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectError(IOException.class)
            .verify(Duration.ofMillis(150));
    }


    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(-1L);

        StepVerifier.create(
            Mono.just("Event")
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectError(RequestNotPermitted.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldEmitRequestNotPermittedExceptionEvenWhenErrorDuringSubscribe() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(-1L);

        StepVerifier.create(
            Mono.error(new IOException("BAM!"))
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectError(RequestNotPermitted.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldDrainRateLimiterInConditionMetOnFailedCall() {
        RateLimiter rateLimiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isFailedAndThrown(callsResult, OverloadException.class))
            .build());

        StepVerifier.create(
            Mono.error(new SpecificOverloadException())
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectError(SpecificOverloadException.class)
            .verify(Duration.ofSeconds(1));
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isZero();
    }

    @Test
    public void shouldDrainRateLimiterInConditionMetOnSuccessfulCall() {
        RateLimiter rateLimiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isSuccessfulAndReturned(
                    callsResult,
                    ResponseWithPotentialOverload.class,
                    ResponseWithPotentialOverload::isOverload))
            .build());
        SpecificResponseWithPotentialOverload response = new SpecificResponseWithPotentialOverload(true);

        StepVerifier.create(
            Mono.just(response)
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectNext(response)
            .verifyComplete();
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isZero();
    }

    @Test
    public void shouldNotDrainRateLimiterInConditionNotMetOnSuccessfulCall() {
        RateLimiter rateLimiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isSuccessfulAndReturned(
                    callsResult,
                    ResponseWithPotentialOverload.class,
                    ResponseWithPotentialOverload::isOverload))
            .build());
        SpecificResponseWithPotentialOverload response = new SpecificResponseWithPotentialOverload(false);

        StepVerifier.create(
            Mono.just(response)
                .transformDeferred(RateLimiterOperator.of(rateLimiter)))
            .expectSubscription()
            .expectNext(response)
            .verifyComplete();
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(4);
    }

}