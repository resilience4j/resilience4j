package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link SingleObserver} to protect another observer by a {@link RateLimiter}.
 * Consumes one permit when subscribed.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class RateLimiterSingleObserver<T> extends DisposableRateLimiter<T> implements SingleObserver<T> {
    private final transient SingleObserver<? super T> childObserver;

    RateLimiterSingleObserver(RateLimiter rateLimiter, SingleObserver<? super T> childObserver) {
        super(rateLimiter);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        onSubscribeWithPermit(disposable);
    }

    @Override
    protected void onSubscribeInner(Disposable disposable) {
        childObserver.onSubscribe(disposable);
    }

    @Override
    public void onSuccess(T value) {
        onSuccessInner(value);
    }

    @Override
    protected void permittedOnSuccess(T value) {
        childObserver.onSuccess(value);
    }

    @Override
    public void onError(Throwable e) {
        onErrorInner(e);
    }

    @Override
    protected void permittedOnError(Throwable e) {
        childObserver.onError(e);
    }
}
