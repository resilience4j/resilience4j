package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.reactor.Permit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a circuit breaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
class CircuitBreakerSubscriber<T> extends Operators.MonoSubscriber<T, T> {

    private final CircuitBreaker circuitBreaker;
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);
    private StopWatch stopWatch;
    private Subscription subscription;

    public CircuitBreakerSubscriber(CircuitBreaker circuitBreaker,
                                    CoreSubscriber<? super T> actual) {
        super(actual);
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (Operators.validate(this.subscription, subscription)) {
            this.subscription = subscription;

            if (acquireCallPermit()) {
                actual.onSubscribe(this);
            } else {
                cancel();
                actual.onSubscribe(this);
                actual.onError(new CircuitBreakerOpenException(
                        String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
            }
        }
    }

    @Override
    public void onNext(T t) {
        requireNonNull(t);

        if (isInvocationPermitted()) {
            actual.onNext(t);
        }
    }

    @Override
    public void onError(Throwable t) {
        requireNonNull(t);

        markFailure(t);
        if (isInvocationPermitted()) {
            actual.onError(t);
        }
    }

    @Override
    public void onComplete() {
        markSuccess();
        if (isInvocationPermitted()) {
            actual.onComplete();
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
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

    private boolean isInvocationPermitted() {
        return !this.isCancelled() && wasCallPermitted();
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
