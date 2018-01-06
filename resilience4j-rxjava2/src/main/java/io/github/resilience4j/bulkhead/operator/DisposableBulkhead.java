package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 */
class DisposableBulkhead extends AtomicReference<Disposable> implements Disposable {
    private final Bulkhead bulkhead;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableBulkhead(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public void dispose() {
        if (DisposableHelper.dispose(this)) {
            releaseBulkhead();
        }
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    protected void onSubscribe(Disposable disposable, Consumer<Disposable> onSubscribe, Consumer<Throwable> onError) {
        DisposableHelper.setOnce(this, disposable);
        if (acquireCallPermit()) {
            onSubscribe.accept(this);
        } else {
            dispose();
            onSubscribe.accept(this);
            onError.accept(bulkheadFullException());
        }
    }

    protected void onError(Throwable e, Consumer<Throwable> onError) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            onError.accept(e);
        }
    }

    protected void onComplete(Action onComplete) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            onComplete.execute();
        }
    }

    protected <T> void onSuccess(T value, Consumer<T> onSuccess) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            onSuccess.accept(value);
        }
    }

    protected boolean isInvocationPermitted() {
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

    protected interface Action {
        void execute();
    }
}
