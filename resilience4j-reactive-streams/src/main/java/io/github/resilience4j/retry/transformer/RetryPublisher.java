package io.github.resilience4j.retry.transformer;

import io.github.resilience4j.retry.Retry;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class RetryPublisher implements Subscriber<Object> {
    private Retry.AsyncContext context;
    public RetryPublisher(Retry retry) {

        this.context = retry.asyncContext();
    }

    @Override
    public void onSubscribe(Subscription s) {

    }

    @Override
    public void onError(Throwable t) {
        context.onError(t);
    }

    @Override
    public void onComplete() {
        context.onComplete();;
    }

    @Override
    public void onNext(Object o) {
        long waitDurationMillis = context.onResult(o);
    }
}
