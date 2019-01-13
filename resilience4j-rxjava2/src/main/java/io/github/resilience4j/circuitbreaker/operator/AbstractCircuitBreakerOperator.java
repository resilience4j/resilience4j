package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.reactivex.exceptions.ProtocolViolationException;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable circuit-breaker acting as a base class for circuit-breaker operators.
 *
 * @param <T>          the type of the emitted event
 * @param <DISPOSABLE> the actual type of the disposable/subscription
 */
abstract class AbstractCircuitBreakerOperator<T, DISPOSABLE> extends AtomicReference<DISPOSABLE> {
    private final CircuitBreaker circuitBreaker;
    private StopWatch stopWatch;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    AbstractCircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    /**
     * Disposes this operator exactly once.
     */
    protected void dispose() {
        disposeOnce();
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
                permittedOnError(circuitBreakerOpenException());
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
        markFailure(e);
        if (isInvocationPermitted()) {
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
        markSuccess();
        if (isInvocationPermitted()) {
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
        markSuccess();
        if (isInvocationPermitted()) {
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

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = circuitBreaker.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            } else {
                stopWatch = StopWatch.start(circuitBreaker.getName());
            }
        }
        return callPermitted;
    }

    private boolean isInvocationPermitted() {
        return !isDisposed() && wasCallPermitted();
    }

    private Exception circuitBreakerOpenException() {
        return new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
    }

    private void markFailure(Throwable e) {
        if (wasCallPermitted()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
        }
    }

    private void markSuccess() {
        if (wasCallPermitted()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
        }
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
