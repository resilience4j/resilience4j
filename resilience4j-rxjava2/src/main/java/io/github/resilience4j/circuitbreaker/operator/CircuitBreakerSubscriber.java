package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link Subscriber} to protect another subscriber by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerSubscriber<T> implements Subscriber<T>, Subscription {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerSubscriber.class);
    private final CircuitBreaker circuitBreaker;
    private final Subscriber<? super T> childSubscriber;
    private Subscription subscription;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private StopWatch stopWatch;

    CircuitBreakerSubscriber(CircuitBreaker circuitBreaker, Subscriber<? super T> childSubscriber) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        LOG.debug("onSubscribe");
        if (circuitBreaker.isCallPermitted()) {
            stopWatch = StopWatch.start(circuitBreaker.getName());
            childSubscriber.onSubscribe(this);
        } else {
            subscription.cancel();
            childSubscriber.onSubscribe(this);
            childSubscriber.onError(new CircuitBreakerOpenException(
                    String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
        }
    }

    @Override
    public void onNext(T event) {
        LOG.debug("onNext: {}", event);
        if (notCancelled()) {
            childSubscriber.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        if (notCancelled()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
            childSubscriber.onError(e);

        }
    }

    @Override
    public void onComplete() {
        LOG.debug("onComplete");
        if (notCancelled()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
            childSubscriber.onComplete();
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        if (notCancelled()) {
            cancelled.set(true);
            subscription.cancel();
        }
    }

    private boolean notCancelled() {
        return !cancelled.get();
    }
}
