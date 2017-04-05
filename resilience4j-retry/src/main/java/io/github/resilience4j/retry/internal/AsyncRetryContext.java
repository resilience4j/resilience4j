package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

import java.util.concurrent.atomic.AtomicInteger;
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

    private final AtomicInteger numOfAttempts = new AtomicInteger(0);

    public AsyncRetryContext(String name, RetryConfig config) {
        this.name = name;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();

        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
        this.metrics = this.new AsyncRetryContextMetrics();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onSuccess() {
        int currentNumOfAttempts = numOfAttempts.get();
        publishRetryEvent(() -> new RetryOnSuccessEvent(name, currentNumOfAttempts, null));
    }

    @Override
    public long onError(Throwable throwable) {
        if (!exceptionPredicate.test(throwable)) {
            return -1;
        }

        int attempt = numOfAttempts.addAndGet(1);

        if (attempt > maxAttempts) {
            return -1;
        }

        publishRetryEvent(() -> new RetryOnErrorEvent(name, attempt, throwable));
        return intervalFunction.apply(attempt);
    }

    @Override
    public Flowable<RetryEvent> getEventStream() {
        return eventPublisher;
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
    public Metrics getMetrics() {
        return this.metrics;
    }

    /**
     * {@inheritDoc}
     */
    public final class AsyncRetryContextMetrics implements Metrics {
        private AsyncRetryContextMetrics() {
        }

        /**
         * @return current number of retry attempts made.
         */
        @Override
        public int getNumAttempts() {
            return numOfAttempts.get();
        }

        /**
         * @return the maximum allowed retries to make.
         */
        @Override
        public int getMaxAttempts() {
            return maxAttempts;
        }
    }
}
