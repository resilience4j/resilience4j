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
package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.reactor.AbstractSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
class BulkheadSubscriber<T> extends AbstractSubscriber<T> {

    private final Bulkhead bulkhead;
    private final boolean singleProducer;

    private final AtomicBoolean eventWasEmitted = new AtomicBoolean(false);
    private final AtomicBoolean successSignaled = new AtomicBoolean(false);

    BulkheadSubscriber(Bulkhead bulkhead,
                                 CoreSubscriber<? super T> downstreamSubscriber,
                                 boolean singleProducer) {
        super(downstreamSubscriber);
        this.bulkhead = requireNonNull(bulkhead);
        this.singleProducer = singleProducer;
    }

    @Override
    public void hookOnNext(T t) {
        if (!isDisposed()) {
            if (singleProducer && successSignaled.compareAndSet( false, true)) {
                bulkhead.onComplete();
            }
            eventWasEmitted.set(true);
            downstreamSubscriber.onNext(t);
        }
    }

    @Override
    public void hookOnCancel() {
        if(!successSignaled.get()){
            if(eventWasEmitted.get()){
                bulkhead.onComplete();
            }else{
                bulkhead.releasePermission();
            }
        }

    }

    @Override
    public void hookOnError(Throwable t) {
        bulkhead.onComplete();
        downstreamSubscriber.onError(t);
    }

    @Override
    public void hookOnComplete() {
        if (successSignaled.compareAndSet( false, true)) {
            bulkhead.onComplete();
        }
        downstreamSubscriber.onComplete();
    }
}
