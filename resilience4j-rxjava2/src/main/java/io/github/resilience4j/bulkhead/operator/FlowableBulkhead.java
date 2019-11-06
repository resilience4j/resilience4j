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
package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.AbstractSubscriber;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

class FlowableBulkhead<T> extends Flowable<T> {

    private final Bulkhead bulkhead;
    private final Publisher<T> upstream;

    FlowableBulkhead(Publisher<T> upstream, Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
        this.upstream = Objects.requireNonNull(upstream, "source is null");
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> downstream) {
        if (bulkhead.tryAcquirePermission()) {
            upstream.subscribe(new BulkheadSubscriber(downstream));
        } else {
            downstream.onSubscribe(EmptySubscription.INSTANCE);
            downstream.onError(BulkheadFullException.createBulkheadFullException(bulkhead));
        }
    }

    class BulkheadSubscriber extends AbstractSubscriber<T> {

        BulkheadSubscriber(Subscriber<? super T> downstreamSubscriber) {
            super(downstreamSubscriber);
        }

        @Override
        public void hookOnError(Throwable t) {
            bulkhead.onComplete();
        }

        @Override
        public void hookOnComplete() {
            bulkhead.onComplete();
        }

        @Override
        public void hookOnCancel() {
            bulkhead.releasePermission();
        }
    }

}