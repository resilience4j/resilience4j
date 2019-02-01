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
import io.github.resilience4j.reactor.ResilienceBaseSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
class BulkheadSubscriber<T> extends ResilienceBaseSubscriber<T> {

    private final Bulkhead bulkhead;

    public BulkheadSubscriber(Bulkhead bulkhead,
                              CoreSubscriber<? super T> actual) {
        super(actual);
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public void hookOnNext(T t) {
        if (notCancelled() && wasCallPermitted()) {
            actual.onNext(t);
        }
    }

    @Override
    public void hookOnCancel() {
        releaseBulkhead();
    }

    @Override
    public void hookOnError(Throwable t) {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
            actual.onError(t);
        }
    }

    @Override
    protected boolean isCallPermitted() {
        return bulkhead.isCallPermitted();
    }

    @Override
    protected Throwable getThrowable() {
        return new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
    }

    @Override
    public void hookOnComplete() {
        if (wasCallPermitted()) {
            releaseBulkhead();
            actual.onComplete();
        }
    }

    private void releaseBulkhead() {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
        }
    }
}
