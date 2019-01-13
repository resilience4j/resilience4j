/*
 * Copyright 2017 Dan Maas
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

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOperator;
import io.reactivex.FlowableOperator;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import org.reactivestreams.Subscriber;

/**
 * A RxJava operator which wraps a reactive type in a rate limiter.
 * All operators consumes one permit at subscription and one permit per emitted event except the first one.
 * Please note that RateLimiter shouldn't use timeout as it may block while acquiring the permit.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class RateLimiterOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T>, CompletableOperator, MaybeOperator<T, T> {
    private final RateLimiter rateLimiter;

    private RateLimiterOperator(RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param rateLimiter the RateLimiter
     * @param <T>         the value type of the upstream and downstream
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return new RateLimiterOperator<>(rateLimiter);
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new RateLimiterSubscriber<>(rateLimiter, childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new RateLimiterObserver<>(rateLimiter, childObserver);
    }

    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new RateLimiterSingleObserver<>(rateLimiter, childObserver);
    }

    @Override
    public CompletableObserver apply(CompletableObserver observer) throws Exception {
        return new RateLimiterCompletableObserver(rateLimiter, observer);
    }

    @Override
    public MaybeObserver<? super T> apply(MaybeObserver<? super T> observer) throws Exception {
        return new RateLimiterMaybeObserver<>(rateLimiter, observer);
    }
}
