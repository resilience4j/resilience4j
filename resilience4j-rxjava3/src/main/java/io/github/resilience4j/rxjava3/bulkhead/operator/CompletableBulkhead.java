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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.rxjava3.AbstractCompletableObserver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

import java.util.concurrent.CompletableFuture;

class CompletableBulkhead extends Completable {

    private final Bulkhead bulkhead;
    private final Completable upstream;

    CompletableBulkhead(Completable upstream, Bulkhead bulkhead) {
        this.upstream = upstream;
        this.bulkhead = bulkhead;
    }

    @Override
    protected void subscribeActual(CompletableObserver downstream) {
        CompletableFuture<Void> permission = bulkhead.acquirePermissionAsync();
        if (permission.isDone()) {
            if (permission.isCompletedExceptionally()) {
                downstream.onSubscribe(EmptyDisposable.INSTANCE);
                downstream.onError(PermissionFailures.of(permission));
            } else {
                upstream.subscribe(new BulkheadCompletableObserver(downstream, null));
            }
        } else {
            PermissionAwaitingDisposable awaiting =
                new PermissionAwaitingDisposable(bulkhead, permission);
            downstream.onSubscribe(awaiting);
            awaiting.await(
                () -> upstream.subscribe(new BulkheadCompletableObserver(downstream, awaiting)),
                downstream::onError);
        }
    }

    class BulkheadCompletableObserver extends AbstractCompletableObserver {

        @Nullable
        private final PermissionAwaitingDisposable awaiting;

        BulkheadCompletableObserver(CompletableObserver downstreamObserver,
            @Nullable PermissionAwaitingDisposable awaiting) {
            super(downstreamObserver);
            this.awaiting = awaiting;
        }

        @Override
        protected void hookOnSubscribe() {
            if (awaiting == null) {
                super.hookOnSubscribe();
            } else {
                awaiting.setUpstream(this);
            }
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
        protected void hookOnCancel() {
            bulkhead.releasePermission();
        }
    }
}
