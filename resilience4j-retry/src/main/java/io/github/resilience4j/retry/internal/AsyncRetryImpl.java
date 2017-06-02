package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.Lazy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AsyncRetryImpl implements AsyncRetry {

    private final String name;
    private final int maxAttempts;
    private final Function<Integer, Long> intervalFunction;
    private final Metrics metrics;
    private final FlowableProcessor<RetryEvent> eventPublisher;
    private final Predicate<Throwable> exceptionPredicate;
    private final RetryConfig config;
    private final Lazy<EventPublisher> lazyEventConsumer;

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

        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
        this.metrics = this.new AsyncRetryMetrics();
        succeededAfterRetryCounter = new LongAdder();
        failedAfterRetryCounter = new LongAdder();
        succeededWithoutRetryCounter = new LongAdder();
        failedWithoutRetryCounter = new LongAdder();
        this.lazyEventConsumer = Lazy.of(() -> new EventDispatcher(getEventStream()));
    }

    public final class ContextImpl implements AsyncRetry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Throwable> lastException = new AtomicReference<>();

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

    @Override
    public EventPublisher getEventPublisher() {
        return lazyEventConsumer.get();
    }

    private class EventDispatcher implements EventPublisher, io.reactivex.functions.Consumer<RetryEvent> {

        private volatile Consumer<RetryOnSuccessEvent> onSuccessEventConsumer;
        private volatile Consumer<RetryOnErrorEvent> onErrorEventConsumer;
        private volatile Consumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer;

        EventDispatcher(Flowable<RetryEvent> eventStream) {
            eventStream.subscribe(this);
        }

        @Override
        public EventPublisher onSuccess(Consumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
            this.onSuccessEventConsumer = onSuccessEventConsumer;
            return this;
        }

        @Override
        public EventPublisher onError(Consumer<RetryOnErrorEvent> onErrorEventConsumer) {
            this.onErrorEventConsumer = onErrorEventConsumer;
            return this;
        }

        @Override
        public EventPublisher onIgnoredError(Consumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
            this.onIgnoredErrorEventConsumer = onIgnoredErrorEventConsumer;
            return this;
        }

        @Override
        public void accept(RetryEvent event) throws Exception {
            RetryEvent.Type eventType = event.getEventType();
            switch (eventType) {
                case SUCCESS:
                    if(onSuccessEventConsumer != null){
                        onSuccessEventConsumer.accept((RetryOnSuccessEvent) event);
                    }
                    break;
                case ERROR:
                    if(onErrorEventConsumer != null) {
                        onErrorEventConsumer.accept((RetryOnErrorEvent) event);
                    }
                    break;
                case IGNORED_ERROR:
                    if(onIgnoredErrorEventConsumer != null) {
                        onIgnoredErrorEventConsumer.accept((RetryOnIgnoredErrorEvent) event);
                    }
                    break;
                default:
                    break;
            }
        }
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
