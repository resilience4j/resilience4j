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
import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

public class RetryTransformer<T> implements FlowableTransformer<T, T>, ObservableTransformer<T, T>,
    SingleTransformer<T, T>, CompletableTransformer, MaybeTransformer<T, T> {

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
        Context<T> context = new Context<>(retry.asyncContext());
        return upstream.doOnNext(context::handleResult)
            .retryWhen(errors -> errors.flatMap(context::handleFlowableErrors))
            .doOnComplete(context::onComplete);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        Context<T> context = new Context<>(retry.asyncContext());
        return upstream.doOnNext(context::handleResult)
            .retryWhen(errors -> errors.flatMap(context::handleObservableErrors))
            .doOnComplete(context::onComplete);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        Context<T> context = new Context<>(retry.asyncContext());
        return upstream.doOnSuccess(context::handleResult)
            .retryWhen(errors -> errors.flatMap(context::handleFlowableErrors))
            .doOnSuccess(t -> context.onComplete());
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        Context<T> context = new Context<>(retry.asyncContext());
        return upstream.retryWhen(errors -> errors.flatMap(context::handleFlowableErrors))
            .doOnComplete(context::onComplete);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        Context<T> context = new Context<>(retry.asyncContext());
        return upstream.doOnSuccess(context::handleResult)
            .retryWhen(errors -> errors.flatMap(context::handleFlowableErrors))
            .doOnSuccess(t -> context.onComplete())
            .doOnComplete(context::onComplete);
    }

    private static class Context<T> {

        private final Retry.AsyncContext<T> retryContext;

        Context(Retry.AsyncContext<T> retryContext) {
            this.retryContext = retryContext;
        }

        void onComplete() {
            this.retryContext.onComplete();
        }

        void handleResult(T result) {
            long waitDurationMillis = retryContext.onResult(result);
            if (waitDurationMillis != -1) {
                throw new RetryDueToResultException(waitDurationMillis);
            }
        }

        Publisher<Long> handleFlowableErrors(Throwable throwable) {
            if (throwable instanceof RetryDueToResultException) {
                long waitDurationMillis = ((RetryDueToResultException) throwable).waitDurationMillis;
                return Flowable.timer(waitDurationMillis, TimeUnit.MILLISECONDS);
            }
            // Filter Error to not retry on it
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }

            long waitDurationMillis = retryContext.onError(throwable);

            if (waitDurationMillis == -1) {
                return Flowable.error(throwable);
            }

            return Flowable.timer(waitDurationMillis, TimeUnit.MILLISECONDS);
        }

        ObservableSource<Long> handleObservableErrors(Throwable throwable) {
            if (throwable instanceof RetryDueToResultException) {
                long waitDurationMillis = ((RetryDueToResultException) throwable).waitDurationMillis;
                return Observable.timer(waitDurationMillis, TimeUnit.MILLISECONDS);
            }
            // Filter Error to not retry on it
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }

            long waitDurationMillis = retryContext.onError(throwable);

            if (waitDurationMillis == -1) {
                return Observable.error(throwable);
            }

            return Observable.timer(waitDurationMillis, TimeUnit.MILLISECONDS);
        }

        private static class RetryDueToResultException extends RuntimeException {
            private final long waitDurationMillis;

            RetryDueToResultException(long waitDurationMillis) {
                super("retry due to retryOnResult predicate");
                this.waitDurationMillis = waitDurationMillis;
            }
        }
    }
}
