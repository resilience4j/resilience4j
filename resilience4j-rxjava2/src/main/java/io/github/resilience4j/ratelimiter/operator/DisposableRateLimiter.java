package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A disposable rate limiter acting as a base class for rate limiter operators.
 *
 * @param <T> the type of the emitted event
 */
abstract class DisposableRateLimiter<T> extends AtomicReference<Disposable> implements Disposable {
    private final transient RateLimiter rateLimiter;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
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
                permittedOnError(rateLimitExceededException());
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

    protected final void onNextInner(T value, boolean firstEvent) {
        if (isInvocationPermitted()) {
            if (firstEvent || rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                permittedOnNext(value);
            } else {
                dispose();
                permittedOnError(rateLimitExceededException());
            }
        }
    }

    private boolean isInvocationPermitted() {
        return !isDisposed() && wasCallPermitted();
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration());
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            }
        }
        return callPermitted;
    }

    private Exception rateLimitExceededException() {
        return new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
