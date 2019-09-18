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

import io.github.resilience4j.AbstractSubscriber;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.createRequestNotPermitted;
import static java.util.Objects.requireNonNull;

class FlowableRateLimiter<T> extends Flowable<T> {

    private final RateLimiter rateLimiter;
    private final Publisher<T> upstream;

    FlowableRateLimiter(Publisher<T> upstream, RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
        this.upstream = Objects.requireNonNull(upstream, "source is null");
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> downstream) {
        long waitDuration = rateLimiter.reservePermission();
        if(waitDuration >= 0){
            if(waitDuration > 0){
                Completable.timer(waitDuration, TimeUnit.NANOSECONDS)
                        .subscribe(() -> upstream.subscribe(new RateLimiterSubscriber(downstream)));
            }else{
                upstream.subscribe(new RateLimiterSubscriber(downstream));
            }
        }else{
            downstream.onSubscribe(EmptySubscription.INSTANCE);
            downstream.onError(createRequestNotPermitted(rateLimiter));
        }
    }

    class RateLimiterSubscriber extends AbstractSubscriber<T> {

        RateLimiterSubscriber(Subscriber<? super T> downstreamSubscriber) {
            super(downstreamSubscriber);
        }

        @Override
        public void hookOnError(Throwable t) {
            // NoOp
        }

        @Override
        public void hookOnComplete() {
            // NoOp
        }

        @Override
        public void hookOnCancel() {
            // NoOp
        }
    }

}