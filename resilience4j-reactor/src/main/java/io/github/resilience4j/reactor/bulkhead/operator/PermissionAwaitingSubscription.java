/*
 * Copyright 2026 Oleksandr Shevchenko
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
import org.reactivestreams.Subscription;
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * A {@link Subscription} which is handed to the downstream subscriber while a bulkhead
 * permission is being acquired asynchronously. Requests are accumulated and replayed once the
 * permission has been granted and the upstream has been subscribed. Cancellation cancels the
 * pending permission request, or releases the permission when it was granted concurrently.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class PermissionAwaitingSubscription<T> extends Operators.DeferredSubscription
    implements CoreSubscriber<T> {

    private final CorePublisher<? extends T> source;
    private final Bulkhead bulkhead;
    private final CoreSubscriber<? super T> actual;
    private final boolean singleProducer;
    private final CompletableFuture<Void> permission;

    PermissionAwaitingSubscription(CorePublisher<? extends T> source, Bulkhead bulkhead,
        CoreSubscriber<? super T> actual, boolean singleProducer,
        CompletableFuture<Void> permission) {
        this.source = source;
        this.bulkhead = bulkhead;
        this.actual = actual;
        this.singleProducer = singleProducer;
        this.permission = permission;
    }

    /**
     * Subscribes the upstream once the permission has been granted, or propagates the failure.
     * Must be called after the downstream received this subscription via
     * {@link CoreSubscriber#onSubscribe(Subscription)}.
     */
    void await() {
        permission.whenComplete((granted, error) -> {
            if (error != null) {
                if (!(error instanceof CancellationException) && !isCancelled()) {
                    actual.onError(error);
                }
            } else {
                // The BulkheadSubscriber releases the permission on all terminal signals. When
                // the downstream cancelled concurrently, set(...) cancels the incoming
                // subscription, which also releases the permission.
                source.subscribe(new BulkheadSubscriber<>(bulkhead, this, singleProducer));
            }
        });
    }

    @Override
    public void cancel() {
        super.cancel();
        permission.cancel(false);
    }

    @Override
    public void onSubscribe(Subscription s) {
        set(s);
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        actual.onComplete();
    }

    @Override
    public Context currentContext() {
        return actual.currentContext();
    }

    /**
     * Extracts the failure of an exceptionally completed permission future.
     */
    static Throwable permissionFailure(CompletableFuture<Void> permission) {
        try {
            permission.join();
            throw new IllegalStateException("Permission future has not failed");
        } catch (CancellationException e) {
            return e;
        } catch (CompletionException e) {
            return e.getCause() != null ? e.getCause() : e;
        }
    }
}
