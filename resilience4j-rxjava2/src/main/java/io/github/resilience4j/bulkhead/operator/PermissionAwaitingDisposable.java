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
package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A {@link Disposable} which is handed to the downstream observer while a bulkhead permission is
 * being acquired asynchronously. Once the permission has been granted the upstream is subscribed
 * and its disposable is routed into this one. Disposing it cancels the pending permission request,
 * or releases the permission when it was granted concurrently.
 */
final class PermissionAwaitingDisposable implements Disposable {

    private final AtomicReference<Disposable> upstream = new AtomicReference<>();
    private final Bulkhead bulkhead;
    private final CompletableFuture<Void> permission;

    PermissionAwaitingDisposable(Bulkhead bulkhead, CompletableFuture<Void> permission) {
        this.bulkhead = bulkhead;
        this.permission = permission;
    }

    /**
     * Subscribes the upstream once the permission has been granted, or signals the failure to the
     * downstream. Must be called after the downstream received this disposable via onSubscribe.
     *
     * @param subscribeUpstream subscribes the upstream with an observer which routes its
     *                          disposable into {@link #setUpstream(Disposable)}
     * @param signalError       signals a permission failure to the downstream
     */
    void await(Runnable subscribeUpstream, Consumer<Throwable> signalError) {
        permission.whenComplete((granted, error) -> {
            if (error != null) {
                if (!(error instanceof CancellationException) && !isDisposed()) {
                    signalError.accept(PermissionFailures.unwrap(error));
                }
            } else if (isDisposed()) {
                // Disposed while the permission was being granted: nothing consumes the upstream,
                // so hand the permission back instead of subscribing and disposing right away.
                bulkhead.releasePermission();
            } else {
                // The bulkhead observer releases the permission on all terminal signals. When the
                // downstream disposes from here on, setUpstream(...) disposes the incoming
                // observer, which also releases the permission.
                subscribeUpstream.run();
            }
        });
    }

    /**
     * Routes the disposable of the subscribed upstream into this one. It is disposed right away
     * when the downstream has already disposed.
     */
    void setUpstream(Disposable disposable) {
        DisposableHelper.setOnce(upstream, disposable);
    }

    @Override
    public void dispose() {
        if (DisposableHelper.dispose(upstream)) {
            permission.cancel(false);
        }
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
    }
}
