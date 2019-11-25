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
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.IllegalPublisherException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.UnaryOperator;


/**
 * A RateLimiter operator which checks if a downstream subscriber/observer can acquire a permission
 * to subscribe to an upstream Publisher. Otherwise emits a {@link RequestNotPermitted} if the rate
 * limit is exceeded.
 *
 * @param <T> the value type
 */
public class RateLimiterOperator<T> implements UnaryOperator<Publisher<T>> {

    private final RateLimiter rateLimiter;

    private RateLimiterOperator(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param rateLimiter the Rate limiter
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return new RateLimiterOperator<>(rateLimiter);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return new MonoRateLimiter<>((Mono<? extends T>) publisher, rateLimiter);
        } else if (publisher instanceof Flux) {
            return new FluxRateLimiter<>((Flux<? extends T>) publisher, rateLimiter);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }
}
