package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.disposables.Disposable;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 */
class DisposableBulkhead implements Disposable {
    private Disposable disposable;
    private final Bulkhead bulkhead;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableBulkhead(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            releaseBulkhead();
            disposable.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    protected void onSubscribe(Disposable disposable, Consumer<Disposable> onSubscribe, Consumer<Throwable> onError) {
        this.disposable = requireNonNull(disposable);
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
