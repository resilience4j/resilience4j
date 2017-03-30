/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.circuitbreaker.operator;


import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.reactivex.FlowableOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava operator which protects an Observable or Flowable by a CircuitBreaker
 * @param <T> the value type of the upstream and downstream
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
     * @param <T> the value type of the upstream and downstream
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker){
        return new CircuitBreakerOperator<>(circuitBreaker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new CircuitBreakerSubscriber(childSubscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new CircuitBreakerObserver(childObserver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new CircuitBreakerSingleObserver(childObserver);
    }

    private final class CircuitBreakerSubscriber implements Subscriber<T>, Subscription{

        private final Subscriber<? super T> childSubscriber;
        private Subscription subscription;
        private AtomicBoolean cancelled = new AtomicBoolean(false);
        private StopWatch stopWatch;

        CircuitBreakerSubscriber(Subscriber<? super T> childSubscriber){
            this.childSubscriber = childSubscriber;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNext(T event) {
            if(LOG.isDebugEnabled()){
                LOG.info("onNext: {}", event);
            }
            if(!isCancelled()) {
                childSubscriber.onNext(event);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!isCancelled()) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), e);
                childSubscriber.onError(e);

            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isCancelled()) {
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                childSubscriber.onComplete();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void request(long n) {
            subscription.request(n);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void cancel() {
            if(!cancelled.get()) {
                cancelled.set(true);
                subscription.cancel();
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final class CircuitBreakerObserver implements Observer<T>, Disposable{

        private final Observer<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);
        private StopWatch stopWatch;

        CircuitBreakerObserver(Observer<? super T> childObserver){
            this.childObserver = childObserver;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNext(T event) {
            if(LOG.isDebugEnabled()){
                LOG.info("onNext: {}", event);
            }
            if(!isDisposed()) {
                childObserver.onNext(event);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!isDisposed()) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), e);
                childObserver.onError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onComplete() {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isDisposed()) {
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                childObserver.onComplete();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            if(!cancelled.get()) {
                cancelled.set(true);
                disposable.dispose();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }
    }

    private class CircuitBreakerSingleObserver implements SingleObserver<T>, Disposable {

        private final SingleObserver<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);
        private StopWatch stopWatch;


        CircuitBreakerSingleObserver(SingleObserver<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if(LOG.isDebugEnabled()){
                LOG.info("onError", e);
            }
            if(!isDisposed()) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), e);
                childObserver.onError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSuccess(T value) {
            if(LOG.isDebugEnabled()){
                LOG.info("onComplete");
            }
            if(!isDisposed()) {
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                childObserver.onSuccess(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            if(!cancelled.get()) {
                cancelled.set(true);
                disposable.dispose();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }
    }
}
