package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava {@link CompletableObserver} to protect another observer by a {@link RateLimiter}.
 * Consumes one permit when subscribed.
 */
final class RateLimiterCompletableObserver extends DisposableRateLimiter implements CompletableObserver {
    private final transient CompletableObserver childObserver;

    RateLimiterCompletableObserver(RateLimiter rateLimiter, CompletableObserver childObserver) {
        super(rateLimiter);
        this.childObserver = childObserver;
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
    public void onComplete() {
        onCompleteInner();
    }

    @Override
    protected void permittedOnComplete() {
        childObserver.onComplete();
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
