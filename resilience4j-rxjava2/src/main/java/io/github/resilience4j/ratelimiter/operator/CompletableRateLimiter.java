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

import io.github.resilience4j.AbstractCompletableObserver;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import java.util.concurrent.TimeUnit;

class CompletableRateLimiter extends Completable {

    private final Completable upstream;
    private final RateLimiter rateLimiter;

    CompletableRateLimiter(Completable upstream, RateLimiter rateLimiter) {
        this.upstream = upstream;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void subscribeActual(CompletableObserver downstream) {
        long waitDuration = rateLimiter.reservePermission();
        if(waitDuration >= 0){
            if(waitDuration > 0){
                Completable.timer(waitDuration, TimeUnit.NANOSECONDS)
                        .subscribe(() -> upstream.subscribe(new RateLimiterCompletableObserver(downstream)));
            }else{
                upstream.subscribe(new RateLimiterCompletableObserver(downstream));
            }
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(RequestNotPermitted.createRequestNotPermitted(rateLimiter));
        }
    }

    class RateLimiterCompletableObserver extends AbstractCompletableObserver {

        RateLimiterCompletableObserver(CompletableObserver downstreamObserver) {
            super(downstreamObserver);
        }

        @Override
        protected void hookOnComplete() {
            // NoOp
        }

        @Override
        protected void hookOnError(Throwable e) {
            // NoOp
        }

        @Override
        protected void hookOnCancel() {
            // NoOp
        }
    }

}
