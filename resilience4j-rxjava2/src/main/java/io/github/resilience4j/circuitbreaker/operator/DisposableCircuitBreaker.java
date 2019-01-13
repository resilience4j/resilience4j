package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.internal.DisposedDisposable;
import io.reactivex.disposables.Disposable;

/**
 * A disposable circuit-breaker.
 *
 * @param <T> the type of the emitted event
 */
abstract class DisposableCircuitBreaker<T> extends AbstractCircuitBreakerOperator<T, Disposable> implements Disposable {

    DisposableCircuitBreaker(CircuitBreaker circuitBreaker) {
        super(circuitBreaker);
    }

    @Override
    public final void dispose() {
        super.dispose();
    }

    @Override
    public final boolean isDisposed() {
        return super.isDisposed();
    }

    @Override
    protected Disposable getDisposedDisposable() {
        return DisposedDisposable.DISPOSED;
    }

    @Override
    protected Disposable currentDisposable() {
        return this;
    }

    @Override
    protected void dispose(Disposable disposable) {
        disposable.dispose();
    }
}
