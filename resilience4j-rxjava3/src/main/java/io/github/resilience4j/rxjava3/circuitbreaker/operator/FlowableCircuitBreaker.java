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

import io.github.resilience4j.rxjava3.AbstractSubscriber;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;
import static java.util.Objects.requireNonNull;

class FlowableCircuitBreaker<T> extends Flowable<T> {

    private final CircuitBreaker circuitBreaker;
    private final Publisher<T> upstream;

    FlowableCircuitBreaker(Publisher<T> upstream, CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.upstream = Objects.requireNonNull(upstream, "source is null");
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> downstream) {
        if (circuitBreaker.tryAcquirePermission()) {
            upstream.subscribe(new CircuitBreakerSubscriber(downstream));
        } else {
            downstream.onSubscribe(EmptySubscription.INSTANCE);
            downstream.onError(createCallNotPermittedException(circuitBreaker));
        }
    }

    class CircuitBreakerSubscriber extends AbstractSubscriber<T> {

        private final long start;

        CircuitBreakerSubscriber(Subscriber<? super T> downstreamSubscriber) {
            super(downstreamSubscriber);
            this.start = System.nanoTime();
        }

        @Override
        public void hookOnError(Throwable t) {
            circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, t);
        }

        @Override
        public void hookOnComplete() {
            circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }

        @Override
        public void hookOnCancel() {
            if (eventWasEmitted.get()) {
                circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            } else {
                circuitBreaker.releasePermission();
            }
        }
    }

}