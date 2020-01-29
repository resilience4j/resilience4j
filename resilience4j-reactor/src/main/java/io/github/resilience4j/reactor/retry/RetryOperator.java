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
import io.vavr.control.Try;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            Context<T> context = new Context<>(retry.asyncContext());
            Mono<T> upstream = (Mono<T>) publisher;
            return upstream.doOnNext(context::handleResult)
                .retryWhen(errors -> errors.flatMap(context::handleErrors))
                .doOnSuccess(t -> context.onComplete());
        } else if (publisher instanceof Flux) {
            Context<T> context = new Context<>(retry.asyncContext());
            Flux<T> upstream = (Flux<T>) publisher;
            return upstream.doOnNext(context::handleResult)
                .retryWhen(errors -> errors.flatMap(context::handleErrors))
                .doOnComplete(context::onComplete);
        } else {
            throw new IllegalPublisherException(publisher);
        }
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
            long waitingDurationMillis = retryContext.onResult(result);
            if (waitingDurationMillis != -1) {
                throw new RetryDueToResultException(waitingDurationMillis);
            }
        }

        Mono<Long> handleErrors(Throwable throwable) {
            if (throwable instanceof RetryDueToResultException) {
                long waitDurationMillis = ((RetryDueToResultException) throwable).waitDurationMillis;
                return Mono.delay(Duration.ofMillis(waitDurationMillis));
            }
            // Filter Error to not retry on it
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }

            long waitingDurationMillis = Try.of(() -> retryContext
                .onError(throwable))
                .get();

            if (waitingDurationMillis == -1) {
                Try.failure(throwable).get();
            }

            return Mono.delay(Duration.ofMillis(waitingDurationMillis));
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
