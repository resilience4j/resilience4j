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
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static io.github.resilience4j.reactor.ratelimiter.operator.MockRateLimiter.CANT_GET_PERMIT;

public class FluxRateLimiterTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();

        StepVerifier.create(
                Flux.just("Event 1", "Event 2", "Event 3")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event 1")
                .expectNext("Event 2")
                .expectNext("Event 3")
                .verifyComplete();

        assertUsedPermits(2);
    }

    @Test
    public void shouldBeRateLimited() {
        for (int i = 0; i < LIMIT_FOR_PERIOD; i++) {
            StepVerifier.create(
                    Flux.just("Event 1", "Event 2")
                            .transform(RateLimiterOperator.of(rateLimiter)))
                    .expectNext("Event 1")
                    .expectNext("Event 2")
                    .verifyComplete();
        }

        StepVerifier.create(Flux.just("Event 1", "Event 2")
                .transform(RateLimiterOperator.of(rateLimiter)))
                .expectError(RequestNotPermitted.class)
                .verify();

        assertUsedPermits(LIMIT_FOR_PERIOD);
    }

    @Test
    public void shouldPropagateError() {
        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));

        assertSinglePermitUsed();
    }

    @Test
    public void shouldEmitRequestNotPermittedException() {
        saturateRateLimiter();

        final RateLimiter mock = new MockRateLimiter(CANT_GET_PERMIT);

        StepVerifier.create(
                Flux.just("Event")
                        .transform(RateLimiterOperator.of(mock)))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }

    @Test
    public void shouldEmitRequestNotPermittedExceptionEvenWhenErrorDuringSubscribe() {
        saturateRateLimiter();

        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }

    @Test
    public void shouldEmitRequestNotPermittedExceptionEvenWhenErrorNotOnSubscribe() {
        saturateRateLimiter();

        StepVerifier.create(
                Flux.error(new IOException("BAM!"), true)
                        .transform(RateLimiterOperator.of(new MockRateLimiter(CANT_GET_PERMIT))))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }
}
