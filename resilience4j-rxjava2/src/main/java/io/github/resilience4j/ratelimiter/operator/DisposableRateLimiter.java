package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.internal.DisposedDisposable;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.disposables.Disposable;

/**
 * A disposable rate limiter acting as a base class for rate limiter operators.
 *
 * @param <T> the type of the emitted event
 */
abstract class DisposableRateLimiter<T> extends AbstractRateLimiterOperator<T, Disposable> implements Disposable {

    DisposableRateLimiter(RateLimiter rateLimiter) {
        super(rateLimiter);
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
