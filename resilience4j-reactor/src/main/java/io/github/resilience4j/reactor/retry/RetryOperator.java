/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.reactor.retry;

import io.github.resilience4j.reactor.IllegalPublisherException;
import io.github.resilience4j.retry.Retry;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A Reactor Retry operator which wraps a reactive type in a Retry.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class RetryOperator<T> implements UnaryOperator<Publisher<T>> {

    private final Retry retry;

    private RetryOperator(Retry retry) {
        this.retry = retry;
    }

    /**
     * Creates a retry.
     *
     * @param <T>   the value type of the upstream and downstream
     * @param retry the retry
     * @return a RetryOperator
     */
    public static <T> RetryOperator<T> of(Retry retry) {
        return new RetryOperator<>(retry);
    }

    /**
     * to handle checked exception handling in reactor Function java 8 doOnNext
     */
    private static <T> Consumer<T> throwingConsumerWrapper(
        ThrowingConsumer<T, Exception> throwingConsumer) {

        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                throw new RetryExceptionWrapper(ex);
            }
        };
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            Context<T> context = new Context<>(retry.context());
            Mono<T> upstream = (Mono<T>) publisher;
            return upstream.doOnNext(context::throwExceptionToForceRetryOnResult)
                .retryWhen(errors -> errors.doOnNext(throwingConsumerWrapper(context::onError)))
                .doOnSuccess(t -> context.onComplete());
        } else if (publisher instanceof Flux) {
            Context<T> context = new Context<>(retry.context());
            Flux<T> upstream = (Flux<T>) publisher;
            return upstream.doOnNext(context::throwExceptionToForceRetryOnResult)
                .retryWhen(errors -> errors.doOnNext(throwingConsumerWrapper(context::onError)))
                .doOnComplete(context::onComplete);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }

    /**
     * @param <T> input
     * @param <E> possible thrown exception
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {

        void accept(T t) throws E;
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
                if (throwable instanceof RetryExceptionWrapper) {
                    context.onError(castToException(throwable.getCause()));
                } else {
                    context.onError(castToException(throwable));
                }

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
