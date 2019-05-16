package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.internal.disposables.EmptyDisposable;

import static java.util.Objects.requireNonNull;

class ObserverCircuitBreakerOperator<T> extends Observable<T> {

    private final Observable<T> upstream;
    private final CircuitBreaker circuitBreaker;

    ObserverCircuitBreakerOperator(Observable<T> upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(Observer<? super T> downstream) {
        if(circuitBreaker.tryAcquirePermission()){
            upstream.subscribe(new CircuitBreakerObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new CallNotPermittedException(circuitBreaker.getName()));
        }
    }
    class CircuitBreakerObserver extends BaseCircuitBreakerObserver implements Observer<T> {

        private final transient Observer<? super T> downstreamObserver;

        CircuitBreakerObserver(Observer<? super T> downstreamObserver) {
            super(circuitBreaker);
            this.downstreamObserver = requireNonNull(downstreamObserver);
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
        }

        @Override
        public void onNext(T item) {
            if (!isDisposed()) {
                downstreamObserver.onNext(item);
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
