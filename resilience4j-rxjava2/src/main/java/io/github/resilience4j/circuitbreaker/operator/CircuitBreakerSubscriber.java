package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.internal.DisposedSubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Subscriber} to protect another subscriber by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerSubscriber<T> extends AbstractCircuitBreakerOperator<T, Subscription> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> childSubscriber;

    CircuitBreakerSubscriber(CircuitBreaker circuitBreaker, Subscriber<? super T> childSubscriber) {
        super(circuitBreaker);
        this.childSubscriber = requireNonNull(childSubscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        onSubscribeWithPermit(subscription);
    }

    @Override
    protected void onSubscribeInner(Subscription subscription) {
        childSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(T value) {
        safeOnNext(value);
    }

    @Override
    protected void permittedOnNext(T value) {
        childSubscriber.onNext(value);
    }

    @Override
    public void onComplete() {
        safeOnComplete();
    }

    @Override
    protected void permittedOnComplete() {
        childSubscriber.onComplete();
    }

    @Override
    public void onError(Throwable e) {
        safeOnError(e);
    }

    @Override
    protected void permittedOnError(Throwable e) {
        childSubscriber.onError(e);
    }

    @Override
    public void request(long n) {
        this.get().request(n);
    }

    @Override
    public void cancel() {
        dispose();
    }

    @Override
    protected Subscription getDisposedDisposable() {
        return DisposedSubscription.CANCELLED;
    }

    @Override
    protected Subscription currentDisposable() {
        return this;
    }

    @Override
    protected void dispose(Subscription disposable) {
        disposable.cancel();
    }
}
