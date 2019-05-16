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

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import static java.util.Objects.requireNonNull;

class SingleCircuitBreaker<T> extends Single<T> {

    private final CircuitBreaker circuitBreaker;
    private final Single<T> upstream;

    SingleCircuitBreaker(Single<T> upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> downstream) {
        if(circuitBreaker.tryAcquirePermission()){
            upstream.subscribe(new CircuitBreakerSingleObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new CallNotPermittedException(circuitBreaker));
        }
    }

    class CircuitBreakerSingleObserver extends BaseCircuitBreakerObserver implements SingleObserver<T> {

        private final SingleObserver<? super T> downstreamObserver;

        CircuitBreakerSingleObserver(SingleObserver<? super T> downstreamObserver) {
            super(circuitBreaker);
            this.downstreamObserver = requireNonNull(downstreamObserver);
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
        }

        @Override
        public void onSuccess(T value) {
            whenNotCompleted(() -> {
                super.onSuccess();
                downstreamObserver.onSuccess(value);
            });
        }

        @Override
        public void onError(Throwable e) {
            whenNotCompleted(() -> {
                super.onError(e);
                downstreamObserver.onError(e);
            });
        }
    }
}
