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

import io.github.resilience4j.AbstractSingleObserver;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.getRequestNotPermitted;

class SingleRateLimiter<T> extends Single<T> {

    private final RateLimiter rateLimiter;
    private final Single<T> upstream;

    SingleRateLimiter(Single<T> upstream, RateLimiter rateLimiter) {
        this.upstream = upstream;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> downstream) {
        long waitDuration = rateLimiter.reservePermission();
        if(waitDuration >= 0){
            if(waitDuration > 0){
                Completable.timer(waitDuration, TimeUnit.NANOSECONDS)
                    .subscribe(() -> upstream.subscribe(new RateLimiterSingleObserver(downstream)));
            }else{
                upstream.subscribe(new RateLimiterSingleObserver(downstream));
            }
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(getRequestNotPermitted(rateLimiter));
        }
    }

    class RateLimiterSingleObserver extends AbstractSingleObserver<T> {

        RateLimiterSingleObserver(SingleObserver<? super T> downstreamObserver) {
            super(downstreamObserver);
        }

        @Override
        protected void hookOnError(Throwable e) {
            // NoOp
        }

        @Override
        protected void hookOnSuccess() {
            // NoOp
        }

        @Override
        protected void hookOnCancel() {
            // NoOp
        }
    }
}
