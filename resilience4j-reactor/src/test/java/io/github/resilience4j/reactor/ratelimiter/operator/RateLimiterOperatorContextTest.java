/*
 * Copyright 2026 Robert Winkler and Resilience4j contributors
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RateLimiterOperatorContextTest {

    @Test
    void shouldPropagateDelayErrorWithoutSubscribingToSource() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        given(rateLimiter.reservePermission(1)).willReturn(Duration.ofMillis(1).toNanos());
        AtomicBoolean subscribed = new AtomicBoolean();
        Mono<String> source = Mono.just("A").doOnSubscribe(ignored -> subscribed.set(true));
        IllegalStateException error = new IllegalStateException("delay failed");
        Mono<Object> failedDelay = Mono.error(error);
        String hookKey = "rateLimiterDelayError";
        Hooks.onEachOperator(hookKey, publisher -> failedDelay);

        try {
            StepVerifier.create(RateLimiterOperator.<String>of(rateLimiter).apply(source))
                .expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error))
                .verify(Duration.ofSeconds(5));

            assertThat(subscribed).isFalse();
        } finally {
            Hooks.resetOnEachOperator(hookKey);
        }
    }

    @ParameterizedTest
    @CsvSource({"false, 0", "false, 1", "true, 0", "true, 1"})
    void shouldPropagateContextToOperatorHooks(boolean flux, long waitMillis) {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        given(rateLimiter.reservePermission(1)).willReturn(Duration.ofMillis(waitMillis).toNanos());
        Queue<Context> observedContexts = new ConcurrentLinkedQueue<>();
        String hookKey = "rateLimiterContext";
        Hooks.onEachOperator(hookKey, Operators.lift((scannable, subscriber) -> {
            observedContexts.add(subscriber.currentContext());
            return subscriber;
        }));

        try {
            CorePublisher<String> source = flux
                ? Flux.deferContextual(context -> Flux.just(context.get("tenant"), "second"))
                : Mono.deferContextual(context -> Mono.just(context.get("tenant")));
            String[] expected = flux ? new String[]{"A", "second"} : new String[]{"A"};

            StepVerifier.create(RateLimiterOperator.<String>of(rateLimiter).apply(source),
                    StepVerifierOptions.create().withInitialContext(Context.of("tenant", "A")))
                .expectNext(expected)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

            assertThat(observedContexts).isNotEmpty().allSatisfy(context ->
                assertThat(context.<String>getOrDefault("tenant", null)).isEqualTo("A"));
        } finally {
            Hooks.resetOnEachOperator(hookKey);
        }
    }
}
