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
package io.github.resilience4j.rxjava3.bulkhead.operator;

import io.github.resilience4j.rxjava3.AbstractObserver;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

class ObserverBulkhead<T> extends Observable<T> {

    private final Observable<T> upstream;
    private final Bulkhead bulkhead;

    ObserverBulkhead(Observable<T> upstream, Bulkhead bulkhead) {
        this.upstream = upstream;
        this.bulkhead = bulkhead;
    }

    @Override
    protected void subscribeActual(Observer<? super T> downstream) {
        if (bulkhead.tryAcquirePermission()) {
            upstream.subscribe(new BulkheadObserver(downstream));
        } else {
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(BulkheadFullException.createBulkheadFullException(bulkhead));
        }
    }

    class BulkheadObserver extends AbstractObserver<T> {

        BulkheadObserver(Observer<? super T> downstreamObserver) {
            super(downstreamObserver);
        }

        @Override
        protected void hookOnError(Throwable e) {
            bulkhead.onComplete();
        }

        @Override
        protected void hookOnComplete() {
            bulkhead.onComplete();
        }

        @Override
        protected void hookOnCancel() {
            bulkhead.releasePermission();
        }
    }

}
