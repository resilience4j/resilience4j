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
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.internal.disposables.EmptyDisposable;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

class ObserverRateLimiter<T> extends Observable<T> {

    private final Observable<T> upstream;
    private final RateLimiter rateLimiter;

    ObserverRateLimiter(Observable<T> upstream, RateLimiter rateLimiter) {
        this.upstream = upstream;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void subscribeActual(Observer<? super T> downstream) {
        if(rateLimiter.acquirePermission(Duration.ZERO)){
            upstream.subscribe(new RateLimiterObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new RequestNotPermitted(rateLimiter));
        }
    }
    class RateLimiterObserver extends BaseRateLimiterObserver implements Observer<T> {

        private final Observer<? super T> downstreamObserver;

        RateLimiterObserver(Observer<? super T> downstreamObserver) {
            super(rateLimiter);
            this.downstreamObserver = requireNonNull(downstreamObserver);
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
        }

        @Override
        public void onNext(T item) {
            if (!isDisposed()) {
                downstreamObserver.onNext(item);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isDisposed()) {
                super.onError(e);
                downstreamObserver.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                super.onSuccess();
                downstreamObserver.onComplete();
            }
        }
    }

}
