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

import io.github.resilience4j.AbstractSingleObserver;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

class SingleBulkhead<T> extends Single<T> {

    private final Bulkhead bulkhead;
    private final Single<T> upstream;

    SingleBulkhead(Single<T> upstream, Bulkhead bulkhead) {
        this.upstream = upstream;
        this.bulkhead = bulkhead;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> downstream) {
        if (bulkhead.tryAcquirePermission()) {
            upstream.subscribe(new BulkheadSingleObserver(downstream));
        } else {
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(BulkheadFullException.createBulkheadFullException(bulkhead));
        }
    }

    class BulkheadSingleObserver extends AbstractSingleObserver<T> {

        BulkheadSingleObserver(SingleObserver<? super T> downstreamObserver) {
            super(downstreamObserver);
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
