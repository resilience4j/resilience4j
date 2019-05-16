package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import static java.util.Objects.requireNonNull;

class MaybeCircuitBreaker<T> extends Maybe<T> {

    private final Maybe<T> upstream;
    private final CircuitBreaker circuitBreaker;

    MaybeCircuitBreaker(Maybe<T> upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> downstream) {
        if(circuitBreaker.tryAcquirePermission()){
            upstream.subscribe(new CircuitBreakerMaybeObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new CallNotPermittedException(circuitBreaker.getName()));
        }
    }

    class CircuitBreakerMaybeObserver extends BaseCircuitBreakerObserver implements MaybeObserver<T> {

        private final MaybeObserver<? super T> downstreamObserver;

        CircuitBreakerMaybeObserver(MaybeObserver<? super T> childObserver) {
            super(circuitBreaker);
            this.downstreamObserver = requireNonNull(childObserver);
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
        }

        @Override
        public void onSuccess(T value) {
            if (!isDisposed()) {
                super.onSuccess();
                downstreamObserver.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isDisposed()) {
                super.onError(e);
                downstreamObserver.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                super.onSuccess();
                downstreamObserver.onComplete();
            }
        }
    }
}
