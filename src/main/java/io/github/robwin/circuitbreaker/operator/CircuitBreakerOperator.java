/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.robwin.circuitbreaker.operator;


import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerOpenException;
import io.github.robwin.metrics.StopWatch;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava operator which protects an Observable or Flowable by a CircuitBreaker
 */
public class CircuitBreakerOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T> {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerOperator.class);

    private final CircuitBreaker circuitBreaker;

    private CircuitBreakerOperator(CircuitBreaker circuitBreaker){
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Creates a CircuitBreakerOperator.
     *
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker){
        return new CircuitBreakerOperator<>(circuitBreaker);
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new CircuitBreakerSubscriber(childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new CircuitBreakerObserver(childObserver);
    }

    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new CircuitBreakerSingleObserver(childObserver);
    }

    private final class CircuitBreakerSubscriber implements Subscriber<T>, Subscription{

        private final Subscriber<? super T> childSubscriber;
        private Subscription subscription;
        private volatile boolean cancelled;
        private StopWatch stopWatch;

        CircuitBreakerSubscriber(Subscriber<? super T> childSubscriber){
            this.childSubscriber = childSubscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            if(LOG.isDebugEnabled()){
                LOG.info("onSubscribe");
            }
            if(circuitBreaker.isCallPermitted()){
                stopWatch = StopWatch.start(circuitBreaker.getName());
                childSubscriber.onSubscribe(this);
            }else{
                subscription.cancel();
                childSubscriber.onSubscribe(this);
                childSubscriber.onError(new CircuitBreakerOpenException(
                        String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
            }
        }

        @Override
        public void onNext(T event) {
            if(LOG.isDebugEnabled()){
                LOG.info("onNext: {}", event);
            }
            if(!cancelled) {
                childSubscriber.onNext(event);
            }
        }

        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!cancelled) {
                circuitBreaker.onError(stopWatch.stop().getElapsedDuration(), e);
                childSubscriber.onError(e);

            }
        }

        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!cancelled) {
                circuitBreaker.onSuccess(stopWatch.stop().getElapsedDuration());
                childSubscriber.onComplete();
            }
        }

        @Override
        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void cancel() {
            if(!cancelled) {
                cancelled = true;
                subscription.cancel();
            }
        }
    }

    private final class CircuitBreakerObserver implements Observer<T>, Disposable{

        private final Observer<? super T> childObserver;
        private Disposable disposable;
        private volatile boolean cancelled;
        private StopWatch stopWatch;

        CircuitBreakerObserver(Observer<? super T> childObserver){
            this.childObserver = childObserver;
        }

        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
            if(LOG.isDebugEnabled()){
                LOG.info("onSubscribe");
            }
            if(circuitBreaker.isCallPermitted()){
                stopWatch = StopWatch.start(circuitBreaker.getName());
                childObserver.onSubscribe(this);
            }else{
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(new CircuitBreakerOpenException(
                        String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
            }
        }

        @Override
        public void onNext(T event) {
            if(LOG.isDebugEnabled()){
                LOG.info("onNext: {}", event);
            }
            if(!isDisposed()) {
                childObserver.onNext(event);
            }
        }

        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!isDisposed()) {
                circuitBreaker.onError(stopWatch.stop().getElapsedDuration(), e);
                childObserver.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isDisposed()) {
                circuitBreaker.onSuccess(stopWatch.stop().getElapsedDuration());
                childObserver.onComplete();
            }
        }

        @Override
        public void dispose() {
            if(!cancelled) {
                cancelled = true;
                disposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }
    }

    private class CircuitBreakerSingleObserver implements SingleObserver<T>, Disposable {

        private final SingleObserver<? super T> childObserver;
        private Disposable disposable;
        private volatile boolean cancelled;
        private StopWatch stopWatch;


        CircuitBreakerSingleObserver(SingleObserver<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
            if(LOG.isDebugEnabled()){
                LOG.info("onSubscribe");
            }
            if(circuitBreaker.isCallPermitted()){
                stopWatch = StopWatch.start(circuitBreaker.getName());
                childObserver.onSubscribe(this);
            }else{
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(new CircuitBreakerOpenException(
                        String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
            }
        }

        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!isDisposed()) {
                circuitBreaker.onError(stopWatch.stop().getElapsedDuration(), e);
                childObserver.onError(e);
            }
        }

        @Override
        public void onSuccess(T value) {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isDisposed()) {
                circuitBreaker.onSuccess(stopWatch.stop().getElapsedDuration());
                childObserver.onSuccess(value);
            }
        }

        @Override
        public void dispose() {
            if(!cancelled) {
                cancelled = true;
                disposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }
    }
}
