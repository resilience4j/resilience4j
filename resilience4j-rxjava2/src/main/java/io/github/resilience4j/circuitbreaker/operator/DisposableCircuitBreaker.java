package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.reactivex.disposables.Disposable;

/**
 * A disposable circuit-breaker.
 */
class DisposableCircuitBreaker implements Disposable {
    private Disposable disposable;
    private final CircuitBreaker circuitBreaker;
    private StopWatch stopWatch;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            disposable.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    protected void setDisposable(Disposable disposable) {
        this.disposable = requireNonNull(disposable);
    }

    protected boolean acquireCallPermit() {
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

    protected boolean isInvocationPermitted() {
        return !isDisposed() && wasCallPermitted();
    }

    protected Exception circuitBreakerOpenException() {
        return new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
    }

    protected void markFailure(Throwable e) {
        if (wasCallPermitted()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
        }
    }

    protected void markSuccess() {
        if (wasCallPermitted()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
        }
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
