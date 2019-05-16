package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

class CompletableCircuitBreaker extends Completable {

    private final Completable upstream;
    private final CircuitBreaker circuitBreaker;

    CompletableCircuitBreaker(Completable upstream, CircuitBreaker circuitBreaker) {
        this.upstream = upstream;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void subscribeActual(CompletableObserver downstream) {
        if(circuitBreaker.tryAcquirePermission()){
            upstream.subscribe(new CircuitBreakerCompletableObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new CallNotPermittedException(circuitBreaker.getName()));
        }
    }

    class CircuitBreakerCompletableObserver extends BaseCircuitBreakerObserver implements CompletableObserver {

        private final CompletableObserver downstreamObserver;

        CircuitBreakerCompletableObserver(CompletableObserver downstreamObserver) {
            super(circuitBreaker);
            this.downstreamObserver = downstreamObserver;
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
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
