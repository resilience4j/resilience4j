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
package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.AbstractSingleObserver;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

class SingleCircuitBreaker<T> extends Single<T> {

    private final CircuitBreaker circuitBreaker;
    private final Single<T> upstream;

    SingleCircuitBreaker(Single<T> upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> downstream) {
        if (circuitBreaker.tryAcquirePermission()) {
            upstream.subscribe(new CircuitBreakerSingleObserver(downstream));
        } else {
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(createCallNotPermittedException(circuitBreaker));
        }
    }

    class CircuitBreakerSingleObserver extends AbstractSingleObserver<T> {

        private final long start;

        CircuitBreakerSingleObserver(SingleObserver<? super T> downstreamObserver) {
            super(downstreamObserver);
            this.start = circuitBreaker.getCurrentTimestamp();
        }

        @Override
        protected void hookOnError(Throwable e) {
            circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit(), e);
        }

        @Override
        protected void hookOnSuccess() {
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit());
        }

        @Override
        protected void hookOnCancel() {
            circuitBreaker.releasePermission();
        }
    }
}
