/*
 * Copyright 2018 Julien Hoarau
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
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.reactor.ResilienceBaseSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a circuit breaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
class CircuitBreakerSubscriber<T> extends ResilienceBaseSubscriber<T> {

    private final CircuitBreaker circuitBreaker;
    @Nullable
    private StopWatch stopWatch;
    private final boolean singleProducer;

    @SuppressWarnings("PMD")
    private volatile int successSignaled = 0;
    private static final AtomicIntegerFieldUpdater<CircuitBreakerSubscriber> SUCCESS_SIGNALED =
            AtomicIntegerFieldUpdater.newUpdater(CircuitBreakerSubscriber.class, "successSignaled");

    public CircuitBreakerSubscriber(CircuitBreaker circuitBreaker,
                                    CoreSubscriber<? super T> actual,
                                    boolean singleProducer) {
        super(actual);
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.singleProducer = singleProducer;
    }

    @Override
    protected void hookOnNext(T value) {
        if (singleProducer && SUCCESS_SIGNALED.compareAndSet(this, 0, 1)) {
            markSuccess();
        }

        if (notCancelled() && wasCallPermitted()) {
            actual.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (SUCCESS_SIGNALED.compareAndSet(this, 0, 1)) {
            markSuccess();
        }

        if (wasCallPermitted()) {
            actual.onComplete();
        }
    }

    @Override
    protected void hookOnError(Throwable t) {
        requireNonNull(t);

        markFailure(t);
        if (wasCallPermitted()) {
            actual.onError(t);
        }
    }

    @Override
    protected void hookOnPermitAcquired() {
        stopWatch = StopWatch.start();
    }

    @Override
    protected boolean isCallPermitted() {
        return circuitBreaker.isCallPermitted();
    }

    @Override
    protected Throwable getThrowable() {
        return new CircuitBreakerOpenException(
                String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
    }

    private void markFailure(Throwable e) {
        if (wasCallPermitted()) {
            circuitBreaker.onError(stopWatch != null ? stopWatch.stop().toNanos() : 0, e);
        }
    }

    private void markSuccess() {
        if (wasCallPermitted()) {
            circuitBreaker.onSuccess(stopWatch != null ? stopWatch.stop().toNanos() : 0);
        }
    }
}
