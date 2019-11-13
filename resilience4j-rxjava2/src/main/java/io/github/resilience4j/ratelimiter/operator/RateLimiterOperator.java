/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.*;
import org.reactivestreams.Publisher;

import static java.util.Objects.requireNonNull;

/**
 * A RateLimiter operator which checks if a downstream subscriber/observer can acquire a permission
 * to subscribe to an upstream Publisher. Otherwise emits a {@link RequestNotPermitted} if the rate
 * limit is exceeded.
 *
 * @param <T> the value type
 */
public class RateLimiterOperator<T> implements FlowableTransformer<T, T>, SingleTransformer<T, T>,
    MaybeTransformer<T, T>, CompletableTransformer, ObservableTransformer<T, T> {

    private final RateLimiter rateLimiter;

    private RateLimiterOperator(RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param rateLimiter the RateLimiter
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return new RateLimiterOperator<>(rateLimiter);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableRateLimiter<>(upstream, rateLimiter);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return new SingleRateLimiter<>(upstream, rateLimiter);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableRateLimiter(upstream, rateLimiter);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return new MaybeRateLimiter<>(upstream, rateLimiter);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObserverRateLimiter<>(upstream, rateLimiter);
    }
}
