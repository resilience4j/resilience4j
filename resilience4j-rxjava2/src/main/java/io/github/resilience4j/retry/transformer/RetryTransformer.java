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
import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryTransformer<T> implements FlowableTransformer<T, T>, ObservableTransformer<T, T>,
        SingleTransformer<T, T>, CompletableTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(RetryTransformer.class);

    private static final RuntimeException RESULT_EXCEPTION = new RuntimeException("retry due to retryOnResult predicate");

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
        return Flowable.fromPublisher(downstream -> applyRetrySubscriber(downstream, upstream));
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return Observable.fromPublisher(downstream ->
                applyRetrySubscriber(downstream, upstream.toFlowable(BackpressureStrategy.BUFFER)));
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return Single.fromPublisher(downstream -> applyRetrySubscriber(downstream, upstream.toFlowable()));
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return Completable.fromPublisher(downstream -> {
            Flowable<T> flowable = upstream.toFlowable();
            SubscriptionArbiter sa = new SubscriptionArbiter(true);
            downstream.onSubscribe(sa);
            //noinspection unchecked
            RetrySubscriber<T> retrySubscriber = new RetrySubscriber<>(downstream,
                    retry.getRetryConfig().getMaxAttempts(), sa, flowable, retry.context(), true);
            flowable.subscribe(retrySubscriber);
        });
    }

    @SuppressWarnings("unchecked")
    private void applyRetrySubscriber(Subscriber<? super T> downstream, Flowable<T> flowable) {
        Retry.Context<T> context = retry.context();
        flowable = flowable.doOnNext(value -> throwExceptionToForceRetryOnResult(context, value));
        SubscriptionArbiter sa = new SubscriptionArbiter(true);
        downstream.onSubscribe(sa);
        RetrySubscriber<T> retrySubscriber = new RetrySubscriber<>(downstream, retry.getRetryConfig().getMaxAttempts(),
                sa, flowable, context, false);
        flowable.subscribe(retrySubscriber);
    }

    private void throwExceptionToForceRetryOnResult(Retry.Context<T> context, T value) {
        if (context.onResult(value))
            throw RESULT_EXCEPTION;
    }

    static final class RetrySubscriber<T> extends AtomicInteger implements Subscriber<T> {

        private final Subscriber<? super T> actual;
        private final SubscriptionArbiter sa;
        private final Publisher<? extends T> source;
        private final Retry.Context<T> context;
        private long remaining;
        private final boolean countCompleteAsSuccess;

        RetrySubscriber(Subscriber<? super T> actual, long count,
                        SubscriptionArbiter sa, Publisher<? extends T> source,
                        Retry.Context<T> context, boolean countCompleteAsSuccess) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.context = context;
            this.remaining = count;
            this.countCompleteAsSuccess = countCompleteAsSuccess;
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
            context.onSuccess();
            actual.onNext(t);
            sa.produced(1L);
        }

        @Override
        public void onError(Throwable t) {
            if (t == RESULT_EXCEPTION) {
                subscribeNext();
                return;
            }
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
                    context.onError((Exception) t);
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
            if (countCompleteAsSuccess) context.onSuccess();
            actual.onComplete();
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        private void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (; ; ) {
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
