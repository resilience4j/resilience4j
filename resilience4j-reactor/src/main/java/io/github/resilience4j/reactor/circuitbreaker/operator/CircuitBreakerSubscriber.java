/*
 * Copyright 2018 Julien Hoarau, Robert Winkler
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
package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.AbstractSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a circuit breaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
class CircuitBreakerSubscriber<T> extends AbstractSubscriber<T> {

    private final CircuitBreaker circuitBreaker;

    private final long start;
    private final boolean singleProducer;

    private final AtomicBoolean successSignaled = new AtomicBoolean(false);
    private final AtomicBoolean eventWasEmitted = new AtomicBoolean(false);

    protected CircuitBreakerSubscriber(CircuitBreaker circuitBreaker,
        CoreSubscriber<? super T> downstreamSubscriber,
        boolean singleProducer) {
        super(downstreamSubscriber);
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.singleProducer = singleProducer;
        this.start = circuitBreaker.getCurrentTimestamp();
    }

    @Override
    protected void hookOnNext(T value) {
        if (!isDisposed()) {
            if (singleProducer && successSignaled.compareAndSet(false, true)) {
                circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit());
            }
            eventWasEmitted.set(true);

            downstreamSubscriber.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (successSignaled.compareAndSet(false, true)) {
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit());
        }

        downstreamSubscriber.onComplete();
    }

    @Override
    public void hookOnCancel() {
        if (!successSignaled.get()) {
            if (eventWasEmitted.get()) {
                circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit());
            } else {
                circuitBreaker.releasePermission();
            }
        }
    }

    @Override
    protected void hookOnError(Throwable e) {
        circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - start, circuitBreaker.getTimestampUnit(), e);
        downstreamSubscriber.onError(e);
    }
}
