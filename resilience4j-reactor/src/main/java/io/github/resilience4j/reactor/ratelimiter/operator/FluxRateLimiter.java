/*
 * Copyright 2018 Julien Hoarau, Robert Winkler
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
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import java.time.Duration;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.createRequestNotPermitted;

class FluxRateLimiter<T> extends FluxOperator<T, T> {

    private final RateLimiter rateLimiter;

    FluxRateLimiter(Flux<? extends T> source, RateLimiter rateLimiter) {
        super(source);
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        long waitDuration = rateLimiter.reservePermission();
        if (waitDuration >= 0) {
            if (waitDuration > 0) {
                Mono.delay(Duration.ofNanos(waitDuration))
                    .subscribe(delay -> source.subscribe(new RateLimiterSubscriber<>(actual)));
            } else {
                source.subscribe(new RateLimiterSubscriber<>(actual));
            }
        } else {
            Operators.error(actual, createRequestNotPermitted(rateLimiter));
        }
    }

}