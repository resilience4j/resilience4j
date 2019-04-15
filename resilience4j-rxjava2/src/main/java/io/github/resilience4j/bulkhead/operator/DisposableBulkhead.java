package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 *
 * @param <T> the type of the emitted event
 */
abstract class DisposableBulkhead<T> extends AtomicReference<Disposable> implements Disposable {
    private final transient Bulkhead bulkhead;
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

    /**
     * onSuccess ensured to be called only when safe.
     *
     * @param disposable the disposable
     */
    protected void onSubscribeInner(Disposable disposable) {
        //Override when needed.
    }

    protected final void onSubscribeWithPermit(Disposable disposable) {
        if (DisposableHelper.setOnce(this, disposable)) {
            if (acquireCallPermit()) {
                onSubscribeInner(this);
            } else {
                dispose();
                onSubscribeInner(this);
                permittedOnError(bulkheadFullException());
            }
        }
    }

    /**
     * onError ensured to be called only when permitted.
     *
     * @param e the error
     */
    protected void permittedOnError(Throwable e) {
        //Override when needed.
    }

    protected final void onErrorInner(Throwable e) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnError(e);
        }
    }

    /**
     * onComplete ensured to be called only when permitted.
     */
    protected void permittedOnComplete() {
        //Override when needed.
    }

    protected final void onCompleteInner() {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnComplete();
        }
    }

    /**
     * onSuccess ensured to be called only when permitted.
     *
     * @param value the value
     */
    protected void permittedOnSuccess(T value) {
        //Override when needed.
    }

    protected final void onSuccessInner(T value) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            permittedOnSuccess(value);
        }
    }

    /**
     * onNext ensured to be called only when permitted.
     *
     * @param value the value
     */
    protected void permittedOnNext(T value) {
        //Override when needed.
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
        if (permitted.compareAndSet(Permit.ACQUIRED, Permit.RELEASED)) {
            bulkhead.onComplete();
        }
    }
}
