package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A RxJava {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class BulkheadSubscriber<T> implements Subscriber<T>, Subscription {
    private final Bulkhead bulkhead;
    private final Subscriber<? super T> childSubscriber;
    private Subscription subscription;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean permitted = new AtomicBoolean(false);

    BulkheadSubscriber(Bulkhead bulkhead, Subscriber<? super T> childSubscriber) {
        this.bulkhead = requireNonNull(bulkhead);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        if (acquireCallPermit()) {
            childSubscriber.onSubscribe(this);
        } else {
            cancel();
            childSubscriber.onSubscribe(this);
            childSubscriber.onError(new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName())));
        }
    }

    @Override
    public void onNext(T event) {
        if (isInvocationPermitted()) {
            childSubscriber.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        if (isInvocationPermitted()) {
            bulkhead.onComplete();
            childSubscriber.onError(e);
        }
    }

    @Override
    public void onComplete() {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            childSubscriber.onComplete();
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            releaseBulkhead();
            subscription.cancel();
        }
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(false, true)) {
            callPermitted = bulkhead.isCallPermitted();
            if (!callPermitted) {
                permitted.set(false);
            }
        }
        return callPermitted;
    }

    private boolean isInvocationPermitted() {
        return !cancelled.get() && wasCallPermitted();
    }

    private boolean wasCallPermitted() {
        return permitted.get();
    }

    private void releaseBulkhead() {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
        }
    }
}
