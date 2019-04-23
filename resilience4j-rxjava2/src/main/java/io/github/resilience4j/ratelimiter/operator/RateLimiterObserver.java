package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Observer} to protect another observer by a {@link RateLimiter}.
 * Consumes one permit when subscribed and one permit per emitted event except the first one.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class RateLimiterObserver<T> extends DisposableRateLimiter<T> implements Observer<T> {
    private final transient Observer<? super T> childObserver;
    private final AtomicBoolean firstEvent = new AtomicBoolean(true);

    RateLimiterObserver(RateLimiter rateLimiter, Observer<? super T> childObserver) {
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
    public void onNext(T value) {
        onNextInner(value, firstEvent.getAndSet(false));
    }

    @Override
    protected void permittedOnNext(T value) {
        childObserver.onNext(value);
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
