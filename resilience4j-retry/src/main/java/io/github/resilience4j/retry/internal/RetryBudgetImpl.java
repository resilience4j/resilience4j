/*
 *
 *  Copyright 2018 Jan Sykora at GoodData(R) Corporation
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

import io.github.resilience4j.bitset.RingBitSet;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.vavr.CheckedConsumer;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryBudgetImpl implements Retry {

    private final Retry.Metrics metrics;
    private final RetryEventProcessor eventProcessor;

    private String name;
    private RetryConfig config;
    private int maxAttempts;
    private Function<Integer, Long> intervalFunction;
    private Predicate<Throwable> exceptionPredicate;
    // to avoid integer division
    private double bufferSize;
    private double retryThreshold;
    private LongAdder succeededAfterRetryCounter;
    private LongAdder failedAfterRetryCounter;
    private LongAdder succeededWithoutRetryCounter;
    private LongAdder failedWithoutRetryCounter;
    private CheckedConsumer<Long> sleepFunction;
    private RingBitSet ringBitSet;

    public RetryBudgetImpl(String name, RetryConfig config) {
        this.name = name;
        this.config = config;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        this.bufferSize = config.getBufferSize();
        this.retryThreshold = config.getRetryThreshold();
        this.eventProcessor = new RetryEventProcessor();
        this.succeededAfterRetryCounter = new LongAdder();
        this.failedAfterRetryCounter = new LongAdder();
        this.succeededWithoutRetryCounter = new LongAdder();
        this.failedWithoutRetryCounter = new LongAdder();
        this.metrics = new RetryMetricsImpl(succeededAfterRetryCounter, failedAfterRetryCounter, succeededWithoutRetryCounter, failedWithoutRetryCounter);
        this.ringBitSet = new RingBitSet(config.getBufferSize());
    }

    public final class ContextImpl implements Retry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Exception> lastException = new AtomicReference<>();
        private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

        private ContextImpl() {
        }

        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            ringBitSet.setNextBit(false);
            if (currentNumOfAttempts > 0) {
                succeededAfterRetryCounter.increment();
                Throwable throwable = Option.of(lastException.get()).getOrElse(lastRuntimeException.get());
                publishRetryEvent(() -> new RetryOnSuccessEvent(getName(), currentNumOfAttempts, throwable));
            } else {
                succeededWithoutRetryCounter.increment();
            }
        }

        public void onError(Exception exception) throws Throwable {
            if (exceptionPredicate.test(exception)) {
                lastException.set(exception);
                throwOrSleepAfterException();
            } else {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), exception));
                throw exception;
            }
        }

        public void onRuntimeError(RuntimeException runtimeException) {
            if (exceptionPredicate.test(runtimeException)) {
                lastRuntimeException.set(runtimeException);
                throwOrSleepAfterRuntimeException();
            } else {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), runtimeException));
                throw runtimeException;
            }
        }

        private void throwOrSleepAfterException() throws Exception {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if (canRetry(currentNumOfAttempts)) {
                waitIntervalAfterFailure();
            } else {
                failedAfterRetryCounter.increment();
                Exception throwable = lastException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }
        }

        private void throwOrSleepAfterRuntimeException() {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if (canRetry(currentNumOfAttempts)) {
                waitIntervalAfterFailure();
            } else {
                failedAfterRetryCounter.increment();
                RuntimeException throwable = lastRuntimeException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }
        }

        private void waitIntervalAfterFailure() {
            // wait interval until the next attempt should start
            long interval = intervalFunction.apply(numOfAttempts.get());
            Try.run(() -> sleepFunction.accept(interval))
                    .getOrElseThrow(ex -> lastRuntimeException.get());
        }

        private boolean canRetry(final int currentNumOfAttempts) {
            if (currentNumOfAttempts >= maxAttempts) {
                ringBitSet.setNextBit(false);
                return false;
            }
            synchronized (this) {
                double retriesPctWithOneMore = (ringBitSet.cardinality() + 1) / bufferSize;
                if (retriesPctWithOneMore >= retryThreshold) {
                    ringBitSet.setNextBit(false);
                    return false;
                }
                ringBitSet.setNextBit(true);
                return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Context context() {
        return new RetryBudgetImpl.ContextImpl();
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
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry.Metrics getMetrics() {
        return this.metrics;
    }
}
