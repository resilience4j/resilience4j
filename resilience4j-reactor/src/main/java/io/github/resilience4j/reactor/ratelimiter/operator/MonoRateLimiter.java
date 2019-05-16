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
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;

class MonoRateLimiter<T> extends MonoOperator<T, T> {
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;

    MonoRateLimiter(Mono<? extends T> source, RateLimiter rateLimiter,
                    Scheduler scheduler) {
        super(source);
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        if(rateLimiter.acquirePermission(Duration.ZERO)){
            source.publishOn(scheduler)
                    .subscribe(new RateLimiterSubscriber<>(actual));
        }else{
            Operators.error(actual, new RequestNotPermitted(rateLimiter));
        }
    }
}
