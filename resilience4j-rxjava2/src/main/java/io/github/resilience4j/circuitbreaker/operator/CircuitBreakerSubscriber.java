package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.core.lang.Nullable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static io.reactivex.internal.subscriptions.SubscriptionHelper.CANCELLED;
import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Subscriber} to protect another subscriber by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerSubscriber<T> extends AtomicReference<Subscription> implements Subscriber<T>, Subscription {
    private final CircuitBreaker circuitBreaker;
    private final Subscriber<? super T> childSubscriber;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);
    @Nullable
    private StopWatch stopWatch;

    CircuitBreakerSubscriber(CircuitBreaker circuitBreaker, Subscriber<? super T> childSubscriber) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (SubscriptionHelper.setOnce(this, subscription)) {
            if (acquireCallPermit()) {
                childSubscriber.onSubscribe(this);
            } else {
                cancel();
                childSubscriber.onSubscribe(this);
                childSubscriber.onError(new RejectedExecutionException(circuitBreaker.getName()));
            }
        }
    }

    @Override
    public void onNext(T event) {
        if (isInvocationPermitted()) {
            childSubscriber.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        markFailure(e);
        if (isInvocationPermitted()) {
            childSubscriber.onError(e);
        }
    }

    @Override
    public void onComplete() {
        markSuccess();
        if (isInvocationPermitted()) {
            childSubscriber.onComplete();
        }
    }

    @Override
    public void request(long n) {
        this.get().request(n);
    }

    @Override
    public void cancel() {
        SubscriptionHelper.cancel(this);
    }

    private boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = circuitBreaker.tryObtainPermission();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            } else {
                stopWatch = StopWatch.start(circuitBreaker.getName());
            }
        }
        return callPermitted;
    }

    private boolean isInvocationPermitted() {
        return notCancelled() && wasCallPermitted();
    }

    private boolean notCancelled() {
        return !(get() == CANCELLED);
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
