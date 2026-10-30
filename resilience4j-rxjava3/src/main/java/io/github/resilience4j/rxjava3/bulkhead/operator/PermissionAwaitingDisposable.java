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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

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
    /**
     * Whether the bulkhead observer has been subscribed to the upstream. From then on it owns the
     * granted permission and releases it on a terminal signal or when it is disposed.
     */
    private volatile boolean upstreamSubscribed;

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
                subscribeUpstream(subscribeUpstream, signalError);
            }
        });
    }

    private void subscribeUpstream(Runnable subscribeUpstream, Consumer<Throwable> signalError) {
        try {
            // The bulkhead observer releases the permission on all terminal signals. When the
            // downstream disposes from here on, setUpstream(...) disposes the incoming
            // observer, which also releases the permission.
            subscribeUpstream.run();
        } catch (Throwable e) {
            // RxJava rethrows an exception thrown by subscribeActual(...) instead of signalling
            // it, and this callback has no caller to rethrow it to. Unless the bulkhead observer
            // was subscribed before the failure, nothing would release the permission and the
            // downstream would never receive a terminal signal.
            Exceptions.throwIfFatal(e);
            onSubscribeFailure(e, signalError);
        }
    }

    private void onSubscribeFailure(Throwable e, Consumer<Throwable> signalError) {
        Disposable subscribed = upstream.get();
        boolean terminated = subscribed != null && subscribed != DisposableHelper.DISPOSED
            && subscribed.isDisposed();
        // Disposes the bulkhead observer when it was subscribed, which releases the permission
        // unless the observer has already terminated.
        boolean active = DisposableHelper.dispose(upstream);
        if (!upstreamSubscribed) {
            bulkhead.releasePermission();
        }
        if (active && !terminated) {
            signalError.accept(e);
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    /**
     * Routes the disposable of the subscribed upstream into this one. It is disposed right away
     * when the downstream has already disposed.
     */
    void setUpstream(Disposable disposable) {
        upstreamSubscribed = true;
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
