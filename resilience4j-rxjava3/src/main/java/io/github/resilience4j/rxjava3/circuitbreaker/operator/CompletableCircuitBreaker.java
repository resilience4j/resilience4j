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
package io.github.resilience4j.rxjava3.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.rxjava3.AbstractCompletableObserver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

class CompletableCircuitBreaker extends Completable {

    private final Completable upstream;
    private final CircuitBreaker circuitBreaker;

    CompletableCircuitBreaker(Completable upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(CompletableObserver downstream) {
        if (circuitBreaker.tryAcquirePermission()) {
            upstream.subscribe(new CircuitBreakerCompletableObserver(downstream));
        } else {
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(createCallNotPermittedException(circuitBreaker));
        }
    }

    class CircuitBreakerCompletableObserver extends AbstractCompletableObserver {

        private final long start;

        CircuitBreakerCompletableObserver(CompletableObserver downstreamObserver) {
            super(downstreamObserver);
            this.start = circuitBreaker.getCurrentTimestamp();
        }

        @Override
        protected void hookOnComplete() {
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit());
        }

        @Override
        protected void hookOnError(Throwable e) {
            circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit(), e);
        }

        @Override
        protected void hookOnCancel() {
            circuitBreaker.releasePermission();
        }
    }

}
