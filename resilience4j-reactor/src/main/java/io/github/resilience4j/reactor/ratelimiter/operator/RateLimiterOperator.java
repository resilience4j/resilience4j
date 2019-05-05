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
import io.github.resilience4j.reactor.FluxResilience;
import io.github.resilience4j.reactor.MonoResilience;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.UnaryOperator;


/**
 * A Reactor operator which wraps a reactive type in a rate limiter.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class RateLimiterOperator<T> implements UnaryOperator<Publisher<T>> {
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;

    private RateLimiterOperator(RateLimiter rateLimiter, Scheduler scheduler) {
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param rateLimiter the Rate limiter
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return of(rateLimiter, Schedulers.parallel());
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param rateLimiter the Rate limiter
     * @param scheduler   the {@link Scheduler} where to publish
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter, Scheduler scheduler) {
        return new RateLimiterOperator<>(rateLimiter, scheduler);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return MonoResilience
                    .onAssembly(new MonoRateLimiter<>((Mono<? extends T>) publisher, rateLimiter, scheduler));
        } else if (publisher instanceof Flux) {
            return FluxResilience
                    .onAssembly(new FluxRateLimiter<>((Flux<? extends T>) publisher, rateLimiter, scheduler));
        }

        throw new IllegalStateException("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> are not supported by this operator");
    }
}
