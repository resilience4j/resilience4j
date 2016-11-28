/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.operator;


import io.reactivex.FlowableOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import javaslang.circuitbreaker.CircuitBreaker;
import javaslang.circuitbreaker.CircuitBreakerOpenException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava operator which protects an Observable or Flowable by a CircuitBreaker
 */
public class CircuitBreakerOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T> {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerOperator.class);

    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerOperator(CircuitBreaker circuitBreaker){
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new CircuitBreakerSubscriber(childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new CircuitBreakerObserver(childObserver);
    }

    private final class CircuitBreakerSubscriber implements Subscriber<T>, Subscription{

        private final Subscriber<? super T> childSubscriber;
        private Subscription subscription;
        private volatile boolean cancelled;

        public CircuitBreakerSubscriber(Subscriber<? super T> childSubscriber){
            this.childSubscriber = childSubscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            if(LOG.isDebugEnabled()){
                LOG.info("onSubscribe");
            }
            if(circuitBreaker.isCallPermitted()){
                childSubscriber.onSubscribe(subscription);
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
                circuitBreaker.recordFailure(e);
                childSubscriber.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!cancelled) {
                circuitBreaker.recordSuccess();
                childSubscriber.onComplete();
            }
        }

        @Override
        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
            subscription.cancel();
        }
    }

    private final class CircuitBreakerObserver implements Observer<T>, Disposable{

        private final Observer<? super T> childObserver;
        private Disposable disposable;
        private volatile boolean cancelled;

        public CircuitBreakerObserver(Observer<? super T> childObserver){
            this.childObserver = childObserver;
        }

        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
            if(LOG.isDebugEnabled()){
                LOG.info("onSubscribe");
            }
            if(circuitBreaker.isCallPermitted()){
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
                circuitBreaker.recordFailure(e);
                childObserver.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isDisposed()) {
                circuitBreaker.recordSuccess();
                childObserver.onComplete();
            }
        }

        @Override
        public void dispose() {
            cancelled = true;
            disposable.dispose();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }
    }
}
