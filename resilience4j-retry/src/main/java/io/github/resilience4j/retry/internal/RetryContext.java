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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.CheckedConsumer;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryContext implements Retry {


    private final Metrics metrics;
    private final FlowableProcessor<RetryEvent> eventPublisher;

    private String name;
    private RetryConfig config;
    private int maxAttempts;
    private Function<Integer, Long> intervalFunction;
    private Predicate<Throwable> exceptionPredicate;
    private LongAdder succeededAfterRetryCounter;
    private LongAdder failedAfterRetryCounter;
    private LongAdder succeededWithoutRetryCounter;
    private LongAdder failedWithoutRetryCounter;
    /*package*/ static CheckedConsumer<Long> sleepFunction = Thread::sleep;

    public RetryContext(String name, RetryConfig config){
        this.name = name;
        this.config = config;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.metrics = this.new RetryMetrics();
        this.eventPublisher = publisher.toSerialized();
        succeededAfterRetryCounter = new LongAdder();
        failedAfterRetryCounter = new LongAdder();
        succeededWithoutRetryCounter = new LongAdder();
        failedWithoutRetryCounter = new LongAdder();
    }

    public final class ContextImpl implements Retry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private AtomicReference<Exception> lastException = new AtomicReference<>();
        private AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

        private ContextImpl() {
        }

        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            if(currentNumOfAttempts > 0){
                succeededAfterRetryCounter.increment();
                Throwable throwable = Option.of(lastException.get()).getOrElse(lastRuntimeException.get());
                publishRetryEvent(() -> new RetryOnSuccessEvent(getName(), currentNumOfAttempts, throwable));
            }else{
                succeededWithoutRetryCounter.increment();
            }
        }

        public void onError(Exception exception) throws Throwable{
            if(exceptionPredicate.test(exception)){
                lastException.set(exception);
                throwOrSleepAfterException();
            }else{
                failedWithoutRetryCounter.increment();
                throw exception;
            }
        }

        public void onRuntimeError(RuntimeException runtimeException){
            if(exceptionPredicate.test(runtimeException)){
                lastRuntimeException.set(runtimeException);
                throwOrSleepAfterRuntimeException();
            }else{
                failedWithoutRetryCounter.increment();
                throw runtimeException;
            }
        }

        private void throwOrSleepAfterException() throws Exception {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if(currentNumOfAttempts >= maxAttempts){
                failedAfterRetryCounter.increment();
                Exception throwable = lastException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }else{
                waitIntervalAfterFailure();
            }
        }

        private void throwOrSleepAfterRuntimeException(){
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if(currentNumOfAttempts >= maxAttempts){
                failedAfterRetryCounter.increment();
                RuntimeException throwable = lastRuntimeException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }else{
                waitIntervalAfterFailure();
            }
        }

        private void waitIntervalAfterFailure() {
            // wait interval until the next attempt should start
            long interval = intervalFunction.apply(numOfAttempts.get());
            Try.run(() -> sleepFunction.accept(interval))
                    .getOrElseThrow(ex -> lastRuntimeException.get());
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
        return new ContextImpl();
    }

    @Override
    public RetryConfig getRetryConfig() {
        return config;
    }


    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(event.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flowable<RetryEvent> getEventStream() {
        return eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    public final class RetryMetrics implements Metrics {
        private RetryMetrics() {
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
}
