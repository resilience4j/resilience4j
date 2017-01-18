package io.github.robwin.retry.internal;

import io.github.robwin.retry.AsyncRetry;
import io.github.robwin.retry.RetryConfig;
import io.github.robwin.retry.event.RetryEvent;
import io.github.robwin.retry.event.RetryOnErrorEvent;
import io.github.robwin.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import javaslang.collection.Stream;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncRetryContext implements AsyncRetry {

    private final String id;
    private final int maxAttempts;
    private Duration waitDuration;
    private final Function<Duration, Duration> backoffFunction;
    private final FlowableProcessor<RetryEvent> eventPublisher;

    private final AtomicInteger numOfAttempts = new AtomicInteger(0);

    public AsyncRetryContext(String id, RetryConfig config) {
        this.id = id;
        this.maxAttempts = config.getMaxAttempts();
        this.backoffFunction = config.getBackoffFunction();
        this.waitDuration = config.getWaitDuration();

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
        int attempt = numOfAttempts.addAndGet(1);
        publishRetryEvent(() -> new RetryOnErrorEvent(id, attempt, throwable));
        return calculateInterval(attempt);
    }

    @Override
    public Flowable<RetryEvent> getEventStream() {
        return eventPublisher;
    }


    private long calculateInterval(int attempt) {

        if (attempt > maxAttempts) {
            return -1;
        } else {
            return Stream.iterate(waitDuration, backoffFunction)
                    .get(attempt - 1)
                    .toMillis();
        }
    }

    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(event.get());
        }
    }
}
