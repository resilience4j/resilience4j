package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link CompletableObserver} to protect another observer by a CircuitBreaker.
 */
final class CircuitBreakerCompletableObserver extends DisposableCircuitBreaker implements CompletableObserver {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerMaybeObserver.class);
    private final CompletableObserver childObserver;

    CircuitBreakerCompletableObserver(CircuitBreaker circuitBreaker, CompletableObserver childObserver) {
        super(circuitBreaker);
        this.childObserver = childObserver;
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        LOG.debug("onSubscribe");
        setDisposable(disposable);
        if (acquireCallPermit()) {
            childObserver.onSubscribe(this);
        } else {
            disposable.dispose();
            childObserver.onSubscribe(this);
            childObserver.onError(circuitBreakerOpenException());
        }
    }

    @Override
    public void onComplete() {
        LOG.debug("onComplete");
        markSuccess();
        if (isInvocationPermitted()) {
            childObserver.onComplete();
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        markFailure(e);
        if (isInvocationPermitted()) {
            childObserver.onError(e);
        }
    }
}
