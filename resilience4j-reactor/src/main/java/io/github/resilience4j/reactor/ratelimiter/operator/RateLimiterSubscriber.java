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
import io.github.resilience4j.reactor.ResilienceBaseSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
class RateLimiterSubscriber<T> extends ResilienceBaseSubscriber<T> {

    private final RateLimiter rateLimiter;
    private final AtomicBoolean firstEvent = new AtomicBoolean(true);

    public RateLimiterSubscriber(RateLimiter rateLimiter,
                                 CoreSubscriber<? super T> actual) {
        super(actual);
        this.rateLimiter = requireNonNull(rateLimiter);
    }

    @Override
    public void hookOnNext(T t) {
        if (notCancelled() && wasCallPermitted()) {
            if (firstEvent.getAndSet(false) || rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                actual.onNext(t);
            } else {
                cancel();
                actual.onError(rateLimitExceededException());
            }
        }
    }

    @Override
    public void hookOnError(Throwable t) {
        if (wasCallPermitted()) {
            actual.onError(t);
        }
    }

    @Override
    protected boolean obtainPermission() {
        return rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }

    @Override
    protected Throwable getThrowable() {
        return rateLimitExceededException();
    }

    @Override
    public void hookOnComplete() {
        if (wasCallPermitted()) {
            actual.onComplete();
        }
    }

    private Exception rateLimitExceededException() {
        return new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
    }
}
