package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AsyncRetryContext implements AsyncRetry {

    private final String name;
    private final int maxAttempts;
    private final Function<Integer, Long> intervalFunction;
    private final Metrics metrics;
    private final FlowableProcessor<RetryEvent> eventPublisher;
    private final Predicate<Throwable> exceptionPredicate;
    private final RetryConfig config;

    private LongAdder succeededAfterRetryCounter;
    private LongAdder failedAfterRetryCounter;
    private LongAdder succeededWithoutRetryCounter;
    private LongAdder failedWithoutRetryCounter;

    public AsyncRetryContext(String name, RetryConfig config) {
        this.config = config;
        this.name = name;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();

        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
        this.metrics = this.new AsyncRetryMetrics();
        succeededAfterRetryCounter = new LongAdder();
        failedAfterRetryCounter = new LongAdder();
        succeededWithoutRetryCounter = new LongAdder();
        failedWithoutRetryCounter = new LongAdder();
    }

    public final class ContextImpl implements AsyncRetry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private AtomicReference<Throwable> lastException = new AtomicReference<>();

        @Override
        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            if(currentNumOfAttempts > 0) {
                publishRetryEvent(() -> new RetryOnSuccessEvent(name, currentNumOfAttempts, lastException.get()));
            }
        }

        @Override
        public long onError(Throwable throwable) {
            if (!exceptionPredicate.test(throwable)) {
                failedWithoutRetryCounter.increment();
                return -1;
            }
            lastException.set(throwable);
            int attempt = numOfAttempts.incrementAndGet();

            if (attempt > maxAttempts) {
                failedAfterRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnErrorEvent(name, attempt, throwable));
                return -1;
            }

            return intervalFunction.apply(attempt);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Flowable<RetryEvent> getEventStream() {
        return eventPublisher;
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

    @Override
    public Metrics getMetrics() {
        return this.metrics;
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
}
