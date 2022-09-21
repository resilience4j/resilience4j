/*
 * Copyright 2021 Robert Winkler and Resilience4j contributors
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
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import java.time.Duration;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.createRequestNotPermitted;

class CorePublisherRateLimiterOperator<T> {

    private final CorePublisher<? extends T> source;
    private final RateLimiter rateLimiter;
    private final int permits;

    CorePublisherRateLimiterOperator(CorePublisher<? extends T> source, RateLimiter rateLimiter, int permits) {
        this.source = source;
        this.rateLimiter = rateLimiter;
        this.permits = permits;
    }

    void subscribe(CoreSubscriber<? super T> actual) {
        long waitDuration = rateLimiter.reservePermission(permits);
        if (waitDuration >= 0) {
            if (waitDuration > 0) {
                delaySubscription(actual, waitDuration);
            } else {
                source.subscribe(new RateLimiterSubscriber<>(rateLimiter, actual));
            }
        } else {
            Operators.error(actual, createRequestNotPermitted(rateLimiter));
        }
    }

    private void delaySubscription(CoreSubscriber<? super T> actual, long waitDuration) {
        Mono.delay(Duration.ofNanos(waitDuration))
            .subscribe(delay -> source.subscribe(
                new RateLimiterSubscriber<>(rateLimiter, actual)));
    }
}
