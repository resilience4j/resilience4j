package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.exceptions.ProtocolViolationException;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 *
 * @param <T>          the type of the emitted event
 * @param <DISPOSABLE> the actual type of the disposable/subscription
 */
abstract class AbstractBulkheadOperator<T, DISPOSABLE> extends AtomicReference<DISPOSABLE> {
    private final Bulkhead bulkhead;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    AbstractBulkheadOperator(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    /**
     * Disposes this operator exactly once.
     */
    protected void dispose() {
        if (disposeOnce()) {
            releaseBulkhead();
        }
    }

    /**
     * Gets whether this operator was already disposed or not.
     *
     * @return true if the operator was disposed, otherwise false
     */
    protected boolean isDisposed() {
        return get() == getDisposedDisposable();
    }

    /**
     * Gets the current disposable.
     *
     * @return the disposable
     */
    protected abstract DISPOSABLE getDisposable();

    /**
     * Gets the reference of the one and only disposed disposable.
     *
     * @return the disposed disposable
     */
    protected abstract DISPOSABLE getDisposedDisposable();

    /**
     * Disposes a disposable.
     *
     * @param disposable the disposable to dispose
     */
    protected abstract void dispose(DISPOSABLE disposable);

    /**
     * onSuccess ensured to be called only when safe.
     *
     * @param disposable the disposable
     */
    protected void onSubscribeInner(DISPOSABLE disposable) {
        //Override when needed.
    }

    protected final void onSubscribeWithPermit(DISPOSABLE disposable) {
        if (setDisposableOnce(disposable)) {
            if (acquireCallPermit()) {
                onSubscribeInner(getDisposable());
            } else {
                dispose();
                onSubscribeInner(getDisposable());
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

    private boolean setDisposableOnce(DISPOSABLE DISPOSABLE) {
        requireNonNull(DISPOSABLE, "DISPOSABLE is null");
        if (!compareAndSet(null, DISPOSABLE)) {
            dispose(DISPOSABLE);
            if (get() != getDisposedDisposable()) {
                RxJavaPlugins.onError(new ProtocolViolationException("Disposable/subscription already set!"));
            }
            return false;
        }
        return true;
    }

    private boolean disposeOnce() {
        DISPOSABLE current = get();
        DISPOSABLE disposed = getDisposedDisposable();
        if (current != disposed) {
            current = getAndSet(disposed);
            if (current != disposed) {
                if (current != null) {
                    dispose(current);
                }
                return true;
            }
        }
        return false;
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
