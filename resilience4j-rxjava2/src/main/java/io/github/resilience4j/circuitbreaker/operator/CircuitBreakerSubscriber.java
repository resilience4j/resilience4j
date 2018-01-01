package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.adapter.Permit;
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
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);
    private StopWatch stopWatch;

    CircuitBreakerSubscriber(CircuitBreaker circuitBreaker, Subscriber<? super T> childSubscriber) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        LOG.debug("onSubscribe");
        if (acquireCallPermit()) {
            childSubscriber.onSubscribe(this);
        } else {
            subscription.cancel();
            childSubscriber.onSubscribe(this);
            childSubscriber.onError(new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
        }
    }

    @Override
    public void onNext(T event) {
        LOG.debug("onNext: {}", event);
        if (isInvocationPermitted()) {
            childSubscriber.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        markFailure(e);
        if (isInvocationPermitted()) {
            childSubscriber.onError(e);
        }
    }

    @Override
    public void onComplete() {
        LOG.debug("onComplete");
        markSuccess();
        if (isInvocationPermitted()) {
            childSubscriber.onComplete();
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            subscription.cancel();
        }
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = circuitBreaker.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            } else {
                stopWatch = StopWatch.start(circuitBreaker.getName());
            }
        }
        return callPermitted;
    }

    protected boolean isInvocationPermitted() {
        return notCancelled() && wasCallPermitted();
    }

    private boolean notCancelled() {
        return !cancelled.get();
    }

    private void markFailure(Throwable e) {
        if (wasCallPermitted()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
        }
    }

    private void markSuccess() {
        if (wasCallPermitted()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
        }
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
