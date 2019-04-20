package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.core.lang.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable circuit-breaker.
 *
 * @param <T> the type of the emitted event
 */
class DisposableCircuitBreaker<T> extends AtomicReference<Disposable> implements Disposable {
    private final transient CircuitBreaker circuitBreaker;
    @Nullable
    private transient StopWatch stopWatch;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    @Override
    public final void dispose() {
        DisposableHelper.dispose(this);
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

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = circuitBreaker.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            } else {
                stopWatch = StopWatch.start();
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
            circuitBreaker.onError(stopWatch != null ? stopWatch.stop().toNanos() : 0, e);
        }
    }

    private void markSuccess() {
        if (wasCallPermitted()) {
            circuitBreaker.onSuccess(stopWatch != null ? stopWatch.stop().toNanos() : 0);
        }
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
