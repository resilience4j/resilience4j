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

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

public class FluxRateLimiterTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();

        assertUsedPermits(2);
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
    public void shouldEmitErrorWithBulkheadFullException() {
        saturateRateLimiter();

        StepVerifier.create(
                Flux.just("Event")
                        .transform(RateLimiterOperator.of(rateLimiter, Schedulers.immediate())))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }
}