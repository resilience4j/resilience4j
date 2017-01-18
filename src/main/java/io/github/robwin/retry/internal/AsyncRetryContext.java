package io.github.robwin.retry.internal;

import io.github.robwin.retry.AsyncRetry;
import io.github.robwin.retry.RetryConfig;
import io.github.robwin.retry.event.RetryEvent;
import io.github.robwin.retry.event.RetryOnErrorEvent;
import io.github.robwin.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AsyncRetryContext implements AsyncRetry {

    private final String id;
    private final int maxAttempts;
    private final Function<Integer, Long> intervalFunction;
    private final FlowableProcessor<RetryEvent> eventPublisher;
    private final Predicate<Throwable> exceptionPredicate;

    private final AtomicInteger numOfAttempts = new AtomicInteger(0);

    public AsyncRetryContext(String id, RetryConfig config) {
        this.id = id;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();

        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void onSuccess() {
        int currentNumOfAttempts = numOfAttempts.get();
        publishRetryEvent(() -> new RetryOnSuccessEvent(id, currentNumOfAttempts, null));
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

        publishRetryEvent(() -> new RetryOnErrorEvent(id, attempt, throwable));
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
}
