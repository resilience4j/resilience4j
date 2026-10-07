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
package io.github.resilience4j.rxjava3.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionArbiter;
import org.reactivestreams.Subscription;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A {@link Subscription} which is handed to the downstream subscriber while a bulkhead permission
 * is being acquired asynchronously. Requests are accumulated and replayed once the permission has
 * been granted and the upstream has been subscribed. Cancellation cancels the pending permission
 * request, or releases the permission when it was granted concurrently.
 */
final class PermissionAwaitingSubscription extends SubscriptionArbiter {

    private static final long serialVersionUID = 1L;

    private final transient Bulkhead bulkhead;
    private final transient CompletableFuture<Void> permission;

    PermissionAwaitingSubscription(Bulkhead bulkhead, CompletableFuture<Void> permission) {
        super(false);
        this.bulkhead = bulkhead;
        this.permission = permission;
    }

    /**
     * Subscribes the upstream once the permission has been granted, or signals the failure to the
     * downstream. Must be called after the downstream received this subscription via onSubscribe.
     *
     * @param subscribeUpstream subscribes the upstream with a subscriber which routes its
     *                          subscription into {@link #setSubscription(Subscription)}
     * @param signalError       signals a permission failure to the downstream
     */
    void await(Runnable subscribeUpstream, Consumer<Throwable> signalError) {
        permission.whenComplete((granted, error) -> {
            if (error != null) {
                if (!(error instanceof CancellationException) && !isCancelled()) {
                    signalError.accept(PermissionFailures.unwrap(error));
                }
            } else if (isCancelled()) {
                // Cancelled while the permission was being granted: nothing consumes the upstream,
                // so hand the permission back instead of subscribing and cancelling right away.
                bulkhead.releasePermission();
            } else {
                // The bulkhead subscriber releases the permission on all terminal signals. When
                // the downstream cancels from here on, setSubscription(...) cancels the incoming
                // subscriber, which also releases the permission.
                subscribeUpstream.run();
            }
        });
    }

    @Override
    public void cancel() {
        super.cancel();
        permission.cancel(false);
    }
}
