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
package io.github.resilience4j.rxjava3;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper.CANCELLED;
import static java.util.Objects.requireNonNull;

public abstract class AbstractSubscriber<T> implements Subscriber<T>, Subscription, Disposable {

    protected final Subscriber<? super T> downstreamSubscriber;
    protected final AtomicBoolean eventWasEmitted = new AtomicBoolean(false);
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();

    protected AbstractSubscriber(Subscriber<? super T> downstreamSubscriber) {
        this.downstreamSubscriber = requireNonNull(downstreamSubscriber);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(subscription, s)) {
            downstreamSubscriber.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T value) {
        if (!isDisposed()) {
            eventWasEmitted.set(true);
            downstreamSubscriber.onNext(value);
        }

    }

    @Override
    public void onError(Throwable t) {
        if (SubscriptionHelper.cancel(subscription)) {
            hookOnError(t);
            downstreamSubscriber.onError(t);
        }
    }

    protected abstract void hookOnError(Throwable t);

    @Override
    public void onComplete() {
        if (SubscriptionHelper.cancel(subscription)) {
            hookOnComplete();
            downstreamSubscriber.onComplete();
        }
    }

    protected abstract void hookOnComplete();

    @Override
    public void request(long n) {
        SubscriptionHelper.validate(n);
        subscription.get().request(n);
    }

    @Override
    public void cancel() {
        if (SubscriptionHelper.cancel(subscription)) {
            hookOnCancel();
        }
    }

    protected abstract void hookOnCancel();

    @Override
    public void dispose() {
        cancel();
    }

    @Override
    public boolean isDisposed() {
        return subscription.get() == CANCELLED;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
