package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;

import static io.reactivex.internal.subscriptions.SubscriptionHelper.CANCELLED;
import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Subscriber} to wrap another subscriber in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class BulkheadSubscriber<T> extends AtomicReference<Subscription> implements Subscriber<T>, Subscription {
    private final transient Bulkhead bulkhead;
    private final transient Subscriber<? super T> childSubscriber;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    BulkheadSubscriber(Bulkhead bulkhead, Subscriber<? super T> childSubscriber) {
        this.bulkhead = requireNonNull(bulkhead);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (SubscriptionHelper.setOnce(this, subscription)) {
            if (acquireCallPermit()) {
                childSubscriber.onSubscribe(this);
            } else {
                cancel();
                childSubscriber.onSubscribe(this);
                childSubscriber.onError(new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName())));
            }
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
        this.get().request(n);
    }

    @Override
    public void cancel() {
        if (SubscriptionHelper.cancel(this)) {
            releaseBulkhead();
        }
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = bulkhead.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            }
        }
        return callPermitted;
    }

    private boolean isInvocationPermitted() {
        return !(get() == CANCELLED) && wasCallPermitted();
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }

    private void releaseBulkhead() {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
        }
    }
}
