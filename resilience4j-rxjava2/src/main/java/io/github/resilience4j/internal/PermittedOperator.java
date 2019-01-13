package io.github.resilience4j.internal;

import io.github.resilience4j.adapter.Permit;
import io.reactivex.exceptions.ProtocolViolationException;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable operator acting as a base class for permitted operators.
 *
 * @param <T>          the type of the emitted event
 * @param <DISPOSABLE> the actual type of the disposable/subscription
 */
public abstract class PermittedOperator<T, DISPOSABLE> extends AtomicReference<DISPOSABLE> {
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    /**
     * Disposes this operator exactly once.
     */
    protected void dispose() {
        if (disposeOnce()) {
            release();
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
    protected abstract DISPOSABLE currentDisposable();

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
     * Tries getting a call permit.
     *
     * @return true if a call permit could be acquired, otherwise false
     */
    protected abstract boolean tryCallPermit();

    /**
     * Creates the exception thrown when the call was not permitted.
     *
     * @return the exception to be thrown
     */
    protected abstract Exception notPermittedException();

    /**
     * Releases any resources (e.g. permits) reserved after a permitted call finishes.
     */
    protected void doOnRelease() {
        // override when needed
    }

    /**
     * onSubscribe ensured to be called only when safe.
     *
     * @param disposable the disposable
     */
    protected abstract void onSubscribeInner(DISPOSABLE disposable);

    /**
     * onSubscribe safe to be called from operator's onSubscribe.
     * Only subscribes if a permit could be acquired.
     *
     * @param disposable the disposable/subscription
     */
    protected final void onSubscribeWithPermit(DISPOSABLE disposable) {
        if (setDisposableOnce(disposable)) {
            if (acquireCallPermit()) {
                onSubscribeInner(currentDisposable());
            } else {
                dispose();
                onSubscribeInner(currentDisposable());
                permittedOnError(notPermittedException());
            }
        }
    }

    /**
     * onError safe to be called from operator's onError.
     *
     * @param e the error thrown
     */
    protected final void safeOnError(Throwable e) {
        if (onlyOnceIfCallWasPermitted()) {
            doOnError(e);
            if (!isDisposed()) {
                permittedOnError(e);
            }
        }
    }

    /**
     * Called if a permitted execution ended up in an error.
     *
     * @param e the error thrown
     */
    protected void doOnError(Throwable e) {
        // Override when needed.
    }

    /**
     * onError ensured to be called only when permitted.
     *
     * @param e the error
     */
    protected void permittedOnError(Throwable e) {
        // Override.
    }

    /**
     * onComplete safe to be called from operator's onComplete.
     */
    protected final void safeOnComplete() {
        if (onlyOnceIfCallWasPermitted()) {
            doOnSuccess();
            if (!isDisposed()) {
                permittedOnComplete();
            }
        }
    }

    /**
     * onComplete ensured to be called only when permitted.
     */
    protected void permittedOnComplete() {
        // Override when needed.
    }


    /**
     * Called if a permitted execution ended successfully.
     */
    protected void doOnSuccess() {
        // override if needed
    }

    /**
     * onSuccess safe to be called from operator's onSuccess.
     *
     * @param value the value
     */
    protected final void safeOnSuccess(T value) {
        if (onlyOnceIfCallWasPermitted()) {
            doOnSuccess();
            if (!isDisposed()) {
                permittedOnSuccess(value);
            }
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

    /**
     * onNext safe to be called from operator's onNext.
     *
     * @param value the value
     */
    protected final void safeOnNext(T value) {
        if (wasCallPermitted() && !isDisposed()) {
            permittedOnNext(value);
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

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = tryCallPermit();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            }
        }
        return callPermitted;
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }

    private boolean onlyOnceIfCallWasPermitted() {
        return permitted.compareAndSet(Permit.ACQUIRED, Permit.RELEASED);
    }

    private void release() {
        if (onlyOnceIfCallWasPermitted()) {
            doOnRelease();
        }
    }
}
