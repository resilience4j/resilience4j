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
package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.reactor.Permit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
class BulkheadSubscriber<T> extends Operators.MonoSubscriber<T, T> {

    private final Bulkhead bulkhead;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    private Subscription subscription;

    public BulkheadSubscriber(Bulkhead bulkhead,
                              CoreSubscriber<? super T> actual) {
        super(actual);
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (Operators.validate(this.subscription, subscription)) {
            this.subscription = subscription;
            if (acquireCallPermit()) {
                actual.onSubscribe(this);
            } else {
                cancel();
                actual.onSubscribe(this);
                actual.onError(new BulkheadFullException(
                        String.format("Bulkhead '%s' is full", bulkhead.getName())));
            }
        }
    }

    @Override
    public void onNext(T t) {
        requireNonNull(t);

        if (isInvocationPermitted()) {
            actual.onNext(t);
        }
    }

    @Override
    public void onError(Throwable t) {
        requireNonNull(t);

        if (isInvocationPermitted()) {
            bulkhead.onComplete();
            actual.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            actual.onComplete();
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = bulkhead.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            }
        }
        return callPermitted;
    }

    private boolean isInvocationPermitted() {
        return notCancelled() && wasCallPermitted();
    }

    private boolean notCancelled() {
        return !this.isCancelled();
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }

    private void releaseBulkhead() {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
        }
    }
}
