package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link MaybeObserver} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerMaybeObserver<T> extends DisposableCircuitBreaker implements MaybeObserver<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerMaybeObserver.class);
    private final MaybeObserver<? super T> childObserver;

    CircuitBreakerMaybeObserver(CircuitBreaker circuitBreaker, MaybeObserver<? super T> childObserver) {
        super(circuitBreaker);
        this.childObserver = requireNonNull(childObserver);
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
    public void onSuccess(T value) {
        LOG.debug("onSuccess: {}", value);
        markSuccess();
        if (isInvocationPermitted()) {
            childObserver.onSuccess(value);
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
