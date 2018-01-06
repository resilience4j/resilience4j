package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 */
abstract class DisposableBulkhead<T> extends AtomicReference<Disposable> implements Disposable {
    private final Bulkhead bulkhead;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableBulkhead(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public final void dispose() {
        if (DisposableHelper.dispose(this)) {
            releaseBulkhead();
        }
    }

    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    protected void onSubscribeInner(Disposable disposable) {
    }

    protected final void permittedOnSubscribe(Disposable disposable) {
        if (DisposableHelper.setOnce(this, disposable)) {
            if (acquireCallPermit()) {
                onSubscribeInner(this);
            } else {
                dispose();
                onSubscribeInner(this);
                permittedOnErrorInner(bulkheadFullException());
            }
        }
    }

    protected void permittedOnErrorInner(Throwable e) {
    }

    protected final void onErrorInner(Throwable e) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnErrorInner(e);
        }
    }

    protected void permittedOnComplete() {
    }

    protected final void onCompleteInner() {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnComplete();
        }
    }

    protected void permittedOnSuccess(T value) {
    }

    protected final void onSuccessInner(T value) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnSuccess(value);
        }
    }

    protected void permittedOnNext(T value) {
    }

    protected final void onNextInner(T value) {
        if (isInvocationPermitted()) {
            permittedOnNext(value);
        }
    }

    private boolean isInvocationPermitted() {
        return !isDisposed() && wasCallPermitted();
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

    private Exception bulkheadFullException() {
        return new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
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
