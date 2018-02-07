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

/**
 * Retry budget implementation of Retry component. The concept was inspired
 * by <a href="https://finagle.github.io/blog/2016/02/08/retry-budgets/">Finagle's retry budget</a>.
 * <p></p>
 * The retry budget uses bit set underneath to mark finished operations with retry. The failed operations with retry
 * are marked as '1's and rest are marked as '0's. The mechanism is described on example below.
 * <p></p>
 * Retry budget with buffer size 5 and retryThreshold set to 0,5.
 * <pre>
 * Initial state:
 *  _x_ _ _ _ _ _ _ _ _
 * |_0_|_0_|_0_|_0_|_0_|
 *
 * First operation fails and allowed retry:
 *  _ _ _x_ _ _ _ _ _ _
 * |_1_|_0_|_0_|_0_|_0_|
 *
 * Second attempt of first operation(the retry is successful):
 *  _ _ _ _ _x_ _ _ _ _
 * |_1_|_0_|_0_|_0_|_0_|
 *
 * Second failed operation and allowed retry:
 *  _ _ _ _ _ _ _x_ _ _
 * |_1_|_0_|_1_|_0_|_0_|
 *
 * Second operation failed also on retry, but retry threshold blocks another attempt and ends in exception:
 *  _ _ _ _ _ _ _ _ _x_
 * |_1_|_0_|_1_|_0_|_0_|
 * </pre>
 * In next operations the index in bit set moves to the front and overwrites the old values.
 * <p></p>
 * The check whether the operation can be retried fetches total number of retries in bit set,
 * adds one (for current operation) and divides it by bit set size (buffer size). This number
 * is compared with the retry threshold. If below the threshold, the retry is permitted and
 * next value in bit set is marked as '1'. If the retry is not allowed the next value is
 * marked as '0'.
 */
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
        this.sleepFunction = config.getSleepFunction();
    }

    public final class ContextImpl implements Retry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Exception> lastException = new AtomicReference<>();
        private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

        private ContextImpl() {
        }

        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            // mark next as '0'
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

        /**
         * Resolves whether the failed operation can retry.
         *
         * @param currentNumOfAttempts current number of attempts by this operation
         * @return boolean whether the operation can retry
         */
        private boolean canRetry(final int currentNumOfAttempts) {
            // check max number of attempts
            if (currentNumOfAttempts >= maxAttempts) {
                // mark next as '0'
                ringBitSet.setNextBit(false);
                return false;
            }
            synchronized (this) {
                // retrieve number of retries in bit set, add one and divided by buffer size
                double retriesPctWithOneMore = (ringBitSet.cardinality() + 1) / bufferSize;
                // check against the threshold
                if (retriesPctWithOneMore >= retryThreshold) {
                    // mark next as '0'
                    ringBitSet.setNextBit(false);
                    return false;
                }
                // mark next as '1'
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
