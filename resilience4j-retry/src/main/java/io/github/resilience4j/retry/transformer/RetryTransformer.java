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

package io.github.resilience4j.retry.transformer;

import io.github.resilience4j.retry.Retry;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryTransformer<T> implements FlowableTransformer<T, T>, ObservableTransformer<T, T>, SingleTransformer<T, T> {

    private static final Logger LOG = LoggerFactory.getLogger(RetryTransformer.class);

    private final Retry retry;

    private RetryTransformer(Retry retry) {
        this.retry = retry;
    }

    /**
     * Creates a RetryOperator.
     *
     * @param retry the Retry
     * @param <T>   the value type of the upstream and downstream
     * @return a RetryOperator
     */
    public static <T> RetryTransformer<T> of(Retry retry) {
        return new RetryTransformer<>(retry);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return Flowable.fromPublisher(d -> {
            SubscriptionArbiter sa = new SubscriptionArbiter();
            d.onSubscribe(sa);
            RetrySubscriber<T> repeatSubscriber = new RetrySubscriber<>(d, retry.getMetrics().getMaxAttempts(), sa, upstream, retry);
            upstream.subscribe(repeatSubscriber);
        });
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return Flowable.<T>fromPublisher(d -> {
            Flowable<T> flowable = upstream.toFlowable(BackpressureStrategy.BUFFER);
            SubscriptionArbiter sa = new SubscriptionArbiter();
            d.onSubscribe(sa);
            RetrySubscriber<T> retrySubscriber = new RetrySubscriber<>(d, retry.getMetrics().getMaxAttempts(), sa, flowable, retry);
            flowable.subscribe(retrySubscriber);
        }).toObservable();
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return Flowable.<T>fromPublisher(d -> {
            Flowable<T> flowable = upstream.toFlowable();
            SubscriptionArbiter sa = new SubscriptionArbiter();
            d.onSubscribe(sa);
            RetrySubscriber<T> retrySubscriber = new RetrySubscriber<>(d, retry.getMetrics().getMaxAttempts(), sa, flowable, retry);
            flowable.subscribe(retrySubscriber);
        }).singleOrError();
    }

    static final class RetrySubscriber<T> extends AtomicInteger implements Subscriber<T> {

        private final Subscriber<? super T> actual;
        private final SubscriptionArbiter sa;
        private final Publisher<? extends T> source;
        private final Retry retry;
        private long remaining;
        RetrySubscriber(Subscriber<? super T> actual, long count,
                         SubscriptionArbiter sa, Publisher<? extends T> source,
                         Retry retry) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.retry = retry;
            this.remaining = count;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onSubscribe");
            }
            sa.setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onNext");
            }
            retry.onSuccess();
            actual.onNext(t);
            sa.produced(1L);
        }
        @Override
        public void onError(Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.info("onError");
            }
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }
            if (r == 0) {
                actual.onError(t);
            } else {
                try {
                    retry.onError((Exception) t);
                    subscribeNext();
                } catch (Throwable t2) {
                    actual.onError(t2);
                }
            }
        }

        @Override
        public void onComplete() {
            if (LOG.isDebugEnabled()) {
                LOG.info("onComplete");
            }
            actual.onComplete();
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        private void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isCancelled()) {
                        return;
                    }
                    source.subscribe(this);

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }

}
