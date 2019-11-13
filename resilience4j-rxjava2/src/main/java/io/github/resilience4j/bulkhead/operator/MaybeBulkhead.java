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

import io.github.resilience4j.AbstractMaybeObserver;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

class MaybeBulkhead<T> extends Maybe<T> {

    private final Maybe<T> upstream;
    private final Bulkhead bulkhead;

    MaybeBulkhead(Maybe<T> upstream, Bulkhead bulkhead) {
        this.upstream = upstream;
        this.bulkhead = bulkhead;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> downstream) {
        if (bulkhead.tryAcquirePermission()) {
            upstream.subscribe(new BulkheadMaybeObserver(downstream));
        } else {
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(BulkheadFullException.createBulkheadFullException(bulkhead));
        }
    }

    class BulkheadMaybeObserver extends AbstractMaybeObserver<T> {

        BulkheadMaybeObserver(MaybeObserver<? super T> downstreamObserver) {
            super(downstreamObserver);
        }

        @Override
        protected void hookOnComplete() {
            bulkhead.onComplete();
        }

        @Override
        protected void hookOnError(Throwable e) {
            bulkhead.onComplete();
        }

        @Override
        protected void hookOnSuccess() {
            bulkhead.onComplete();
        }

        @Override
        protected void hookOnCancel() {
            bulkhead.releasePermission();
        }
    }
}
