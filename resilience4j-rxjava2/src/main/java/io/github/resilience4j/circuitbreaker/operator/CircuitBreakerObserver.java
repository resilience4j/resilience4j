package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Observer} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerObserver<T> extends DisposableCircuitBreaker<T> implements Observer<T> {
    private final transient Observer<? super T> childObserver;

    CircuitBreakerObserver(CircuitBreaker circuitBreaker, Observer<? super T> childObserver) {
        super(circuitBreaker);
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
        onNextInner(value);
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
