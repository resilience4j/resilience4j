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
        Context<T> context = new Context<>(retry.context());
        return upstream.doOnNext(context::throwExceptionToForceRetryOnResult)
            .retryWhen(errors -> errors.doOnNext(context::onError))
            .doOnComplete(context::onComplete);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        Context<T> context = new Context<>(retry.context());
        return upstream.doOnNext(context::throwExceptionToForceRetryOnResult)
            .retryWhen(errors -> errors.doOnNext(context::onError))
            .doOnComplete(context::onComplete);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        Context<T> context = new Context<>(retry.context());
        return upstream.doOnSuccess(context::throwExceptionToForceRetryOnResult)
            .retryWhen(errors -> errors.doOnNext(context::onError))
            .doOnSuccess(t -> context.onComplete());
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        Context<T> context = new Context<>(retry.context());
        return upstream.retryWhen(errors -> errors.doOnNext(context::onError))
            .doOnComplete(context::onComplete);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        Context<T> context = new Context<>(retry.context());
        return upstream.doOnSuccess(context::throwExceptionToForceRetryOnResult)
            .retryWhen(errors -> errors.doOnNext(context::onError))
            .doOnSuccess(t -> context.onComplete())
            .doOnComplete(context::onComplete);
    }

    private static class Context<T> {

        private final Retry.Context<T> context;

        Context(Retry.Context<T> context) {
            this.context = context;
        }

        void onComplete() {
            this.context.onComplete();
        }

        void throwExceptionToForceRetryOnResult(T value) {
            if (context.onResult(value)) {
                throw new RetryDueToResultException();
            }
        }

        void onError(Throwable throwable) throws Exception {
            if (throwable instanceof RetryDueToResultException) {
                return;
            }
            // Filter Error to not retry on it
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            try {
                context.onError(castToException(throwable));
            } catch (Throwable t) {
                throw castToException(t);
            }
        }

        private Exception castToException(Throwable throwable) {
            return throwable instanceof Exception ? (Exception) throwable
                : new Exception(throwable);
        }

        private static class RetryDueToResultException extends RuntimeException {

            RetryDueToResultException() {
                super("retry due to retryOnResult predicate");
            }
        }
    }
}
