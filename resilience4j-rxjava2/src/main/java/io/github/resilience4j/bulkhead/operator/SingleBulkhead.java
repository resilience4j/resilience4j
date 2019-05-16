package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import static java.util.Objects.requireNonNull;

class SingleCircuitBreaker<T> extends Single<T> {

    private final CircuitBreaker circuitBreaker;
    private final Single<T> upstream;

    SingleCircuitBreaker(Single<T> upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> downstream) {
        if(circuitBreaker.tryAcquirePermission()){
            upstream.subscribe(new CircuitBreakerSingleObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new CallNotPermittedException(circuitBreaker.getName()));
        }
    }

    class CircuitBreakerSingleObserver extends BaseCircuitBreakerObserver implements SingleObserver<T> {

        private final SingleObserver<? super T> downstreamObserver;

        CircuitBreakerSingleObserver(SingleObserver<? super T> downstreamObserver) {
            super(circuitBreaker);
            this.downstreamObserver = requireNonNull(downstreamObserver);
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
    }
}
