/*
 * Copyright 2019 authors
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
package io.github.resilience4j.reactor.timelimiter;

import io.github.resilience4j.reactor.IllegalPublisherException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.UnaryOperator;

/**
 * A Reactor TimeLimiter operator which wraps a reactive type in a TimeLimiter.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class TimeLimiterOperator<T> implements UnaryOperator<Publisher<T>> {

    private final TimeLimiter timeLimiter;

    private TimeLimiterOperator(TimeLimiter timeLimiter) {
        this.timeLimiter = timeLimiter;
    }

    /**
     * Creates a timeLimiter.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param timeLimiter the timeLimiter
     * @return a TimeLimiterOperator
     */
    public static <T> TimeLimiterOperator<T> of(TimeLimiter timeLimiter) {
        return new TimeLimiterOperator<>(timeLimiter);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return withTimeout((Mono<T>) publisher);
        } else if (publisher instanceof Flux) {
            return withTimeout((Flux<T>) publisher);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }

    private Publisher<T> withTimeout(Mono<T> upstream) {
        return upstream.timeout(getTimeout())
            .doOnNext(t -> timeLimiter.onSuccess())
            .doOnSuccess(t -> timeLimiter.onSuccess())
            .doOnError(timeLimiter::onError);
    }

    private Publisher<T> withTimeout(Flux<T> upstream) {
        return upstream.timeout(getTimeout())
            .doOnNext(t -> timeLimiter.onSuccess())
            .doOnComplete(timeLimiter::onSuccess)
            .doOnError(timeLimiter::onError);
    }

    private Duration getTimeout() {
        return timeLimiter.getTimeLimiterConfig().getTimeoutDuration();
    }

}
