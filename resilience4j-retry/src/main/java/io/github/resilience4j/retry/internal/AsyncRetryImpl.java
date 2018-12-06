/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.retry.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;

public class AsyncRetryImpl<T> implements AsyncRetry {

    private final String name;
    private final int maxAttempts;
    private final Function<Integer, Long> intervalFunction;
    private final Metrics metrics;
    private final Predicate<Throwable> exceptionPredicate;
    private final RetryConfig config;
    private final RetryEventProcessor eventProcessor;
    private final Predicate<T> resultPredicate;

    private LongAdder succeededAfterRetryCounter;
    private LongAdder failedAfterRetryCounter;
    private LongAdder succeededWithoutRetryCounter;
    private LongAdder failedWithoutRetryCounter;

    public AsyncRetryImpl(String name, RetryConfig config) {
        this.config = config;
        this.name = name;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        this.resultPredicate = config.getResultPredicate();
        this.metrics = this.new AsyncRetryMetrics();
        succeededAfterRetryCounter = new LongAdder();
        failedAfterRetryCounter = new LongAdder();
        succeededWithoutRetryCounter = new LongAdder();
        failedWithoutRetryCounter = new LongAdder();
        this.eventProcessor = new RetryEventProcessor();
    }

    public final class ContextImpl implements AsyncRetry.Context<T> {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Throwable> lastException = new AtomicReference<>();

        @Override
        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            if (currentNumOfAttempts > 0) {
                succeededAfterRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnSuccessEvent(name, currentNumOfAttempts, lastException.get()));
            } else {
                succeededWithoutRetryCounter.increment();
            }
        }

        @Override
        public long onError(Throwable throwable) {
            if (!exceptionPredicate.test(throwable)) {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), throwable));
                return -1;
            }
            lastException.set(throwable);
            int attempt = numOfAttempts.incrementAndGet();

            if (attempt >= maxAttempts) {
                failedAfterRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnErrorEvent(name, attempt, throwable));
                return -1;
            }

            long interval = intervalFunction.apply(attempt);
            publishRetryEvent(() -> new RetryOnRetryEvent(getName(), attempt, throwable, interval));
            return interval;
        }

        @Override
        public long onResult(T result) {
            if (null != resultPredicate && resultPredicate.test(result)) {
                int attempt = numOfAttempts.incrementAndGet();
                if (attempt >= maxAttempts) {
                    return -1;
                }
                return intervalFunction.apply(attempt);
            } else {
                return -1;
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Context context() {
        return new ContextImpl();
    }

    @Override
    public RetryConfig getRetryConfig() {
        return config;
    }


    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(event.get());
        }
    }

    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }


    public final class AsyncRetryMetrics implements AsyncRetry.Metrics {
        private AsyncRetryMetrics() {
        }

        @Override
        public long getNumberOfSuccessfulCallsWithoutRetryAttempt() {
            return succeededWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithoutRetryAttempt() {
            return failedWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfSuccessfulCallsWithRetryAttempt() {
            return succeededAfterRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithRetryAttempt() {
            return failedAfterRetryCounter.longValue();
        }
    }

    private class RetryEventProcessor extends EventProcessor<RetryEvent> implements EventConsumer<RetryEvent>, EventPublisher {

        @Override
        public void consumeEvent(RetryEvent event) {
            super.processEvent(event);
        }

        @Override
        public EventPublisher onRetry(EventConsumer<RetryOnRetryEvent> onRetryEventConsumer) {
            registerConsumer(RetryOnRetryEvent.class, onRetryEventConsumer);
            return this;
        }

        @Override
        public AsyncRetry.EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
            registerConsumer(RetryOnSuccessEvent.class, onSuccessEventConsumer);
            return this;
        }

        @Override
        public AsyncRetry.EventPublisher onError(EventConsumer<RetryOnErrorEvent> onErrorEventConsumer) {
            registerConsumer(RetryOnErrorEvent.class, onErrorEventConsumer);
            return this;
        }

        @Override
        public AsyncRetry.EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
            registerConsumer(RetryOnIgnoredErrorEvent.class, onIgnoredErrorEventConsumer);
            return this;
        }
    }
}
