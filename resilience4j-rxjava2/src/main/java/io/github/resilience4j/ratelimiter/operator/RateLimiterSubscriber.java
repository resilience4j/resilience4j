package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.internal.DisposedSubscription;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link Subscriber} to protect another subscriber by a {@link RateLimiter}.
 * Consumes one permit when subscribed and one permit per emitted event except the first one.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class RateLimiterSubscriber<T> extends AbstractRateLimiterOperator<T, Subscription> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> childSubscriber;
    private final AtomicBoolean firstEvent = new AtomicBoolean(true);

    RateLimiterSubscriber(RateLimiter rateLimiter, Subscriber<? super T> childSubscriber) {
        super(rateLimiter);
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
        if (firstEvent.getAndSet(false) || tryCallPermit()) {
            childSubscriber.onNext(value);
        } else {
            cancel();
            childSubscriber.onError(notPermittedException());
        }
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
