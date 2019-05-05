package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava {@link CompletableObserver} to protect another observer by a CircuitBreaker.
 */
final class CircuitBreakerCompletableObserver extends DisposableCircuitBreaker implements CompletableObserver {
    private final transient CompletableObserver childObserver;

    CircuitBreakerCompletableObserver(CircuitBreaker circuitBreaker, CompletableObserver childObserver) {
        super(circuitBreaker);
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
