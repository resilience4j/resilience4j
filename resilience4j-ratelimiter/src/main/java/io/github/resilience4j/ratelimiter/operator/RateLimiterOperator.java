/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.FlowableOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class RateLimiterOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T> {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterOperator.class);

    private final RateLimiter rateLimiter;

    private RateLimiterOperator(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param rateLimiter the RateLimiter
     * @param <T>         the value type of the upstream and downstream
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return new RateLimiterOperator<>(rateLimiter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new RateLimiterSubscriber(childSubscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new RateLimiterObserver(childObserver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new RateLimiterSingleObserver(childObserver);
    }

    private final class RateLimiterSubscriber implements Subscriber<T>, Subscription {

        private final Subscriber<? super T> childSubscriber;
        private Subscription subscription;
        private AtomicBoolean cancelled = new AtomicBoolean(false);

        RateLimiterSubscriber(Subscriber<? super T> childSubscriber) {
            this.childSubscriber = childSubscriber;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            if (LOG.isDebugEnabled()) {
                LOG.info("onSubscribe");
            }
            if (rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                childSubscriber.onSubscribe(this);
            } else {
                subscription.cancel();
                childSubscriber.onSubscribe(this);
                childSubscriber.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNext(T event) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onNext: {}", event);
            }
            if (!isCancelled()) {
                if (rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                    childSubscriber.onNext(event);
                } else {
                    subscription.cancel();
                    childSubscriber.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onError", e);
            }
            if (!isCancelled()) {
                childSubscriber.onError(e);

            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onComplete() {
            if (LOG.isDebugEnabled()) {
                LOG.info("onComplete");
            }
            if (!isCancelled()) {
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
            if (!cancelled.get()) {
                cancelled.set(true);
                subscription.cancel();
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final class RateLimiterObserver implements Observer<T>, Disposable {

        private final Observer<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);

        RateLimiterObserver(Observer<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
            if (LOG.isDebugEnabled()) {
                LOG.info("onSubscribe");
            }
            if (rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                childObserver.onSubscribe(this);
            } else {
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNext(T event) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onNext: {}", event);
            }
            if (!isDisposed()) {
                if (rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                    childObserver.onNext(event);
                } else {
                    disposable.dispose();
                    childObserver.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onError", e);
            }
            if (!isDisposed()) {
                childObserver.onError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onComplete() {
            if (LOG.isDebugEnabled()) {
                LOG.info("onComplete");
            }
            if (!isDisposed()) {
                childObserver.onComplete();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            if (!cancelled.get()) {
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

    private class RateLimiterSingleObserver implements SingleObserver<T>, Disposable {

        private final SingleObserver<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);


        RateLimiterSingleObserver(SingleObserver<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
            if (LOG.isDebugEnabled()) {
                LOG.info("onSubscribe");
            }
            if (rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration())) {
                childObserver.onSubscribe(this);
            } else {
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable e) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onError", e);
            }
            if (!isDisposed()) {
                childObserver.onError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSuccess(T value) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onComplete");
            }
            if (!isDisposed()) {
                childObserver.onSuccess(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            if (!cancelled.get()) {
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
