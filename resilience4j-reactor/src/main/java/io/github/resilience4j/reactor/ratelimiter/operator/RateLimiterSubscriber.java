package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Scheduler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber with a rate limiter.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class RateLimiterSubscriber<T> implements CoreSubscriber<T>, Subscription, Disposable {

    private static final AtomicLongFieldUpdater<RateLimiterSubscriber> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(RateLimiterSubscriber.class, "requested");
    private volatile long requested;

    private static final AtomicReferenceFieldUpdater<RateLimiterSubscriber, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(RateLimiterSubscriber.class, Subscription.class, "subscription");

    @Nullable
    private volatile Subscription subscription;
    private volatile Status status;

    private final CoreSubscriber<? super T> actual;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final boolean subscribedToSinglePublisher;

    private final DrainRunnable drainRunnable;

    public RateLimiterSubscriber(CoreSubscriber<? super T> actual, RateLimiter rateLimiter, Scheduler scheduler, boolean subscribedToSinglePublisher) {
        this.actual = actual;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.subscribedToSinglePublisher = subscribedToSinglePublisher;
        this.drainRunnable = new DrainRunnable();
        this.status = Status.RUNNING;
    }

    @Override
    public void request(long n) {
        if (Operators.validate(n)) {
            Operators.addCap(REQUESTED, this, n);
            drain();
        }
    }

    private void drain() {
        long r = requested;
        if (r > 0 && status == Status.RUNNING) {
            final long timeToWait = rateLimiter.reservePermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration());
            if (timeToWait < 0) {
                handleError(Operators.onOperatorError(subscription, rateLimitExceededException(), actual.currentContext()));
                status = Status.REJECTED;
                return;
            }

            if (timeToWait == 0) {
                drainRunnable.run();
            } else {
                scheduler.schedule(drainRunnable, timeToWait, TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public void cancel() {
        Operators.terminate(S, this);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (Operators.validate(subscription, s)) {
            this.subscription = s;
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        Objects.requireNonNull(t, "onNext");

        actual.onNext(t);

        if (!subscribedToSinglePublisher) {
            drain();
        }
    }

    private void handleError(Throwable t) {
        if (S.getAndSet(this, Operators.cancelledSubscription()) == Operators
                .cancelledSubscription()) {
            if (status == Status.REJECTED) {
                return;
            }

            Operators.onErrorDropped(t, currentContext());
            return;
        }

        try {
            actual.onError(t);
        } catch (Throwable e) {
            e = Exceptions.addSuppressed(e, t);
            Operators.onErrorDropped(e, currentContext());
        }
    }

    @Override
    public void onError(Throwable t) {
        Objects.requireNonNull(t, "onError");
        handleError(t);
    }

    @Override
    public void onComplete() {
        if (S.getAndSet(this, Operators.cancelledSubscription()) != Operators
                .cancelledSubscription()) {
            try {
                status = Status.DONE;
                actual.onComplete();
            } catch (Throwable throwable) {
                actual.onError(Operators.onOperatorError(throwable, currentContext()));
            }
        }
    }

    @Override
    public void dispose() {
        cancel();
    }

    private Exception rateLimitExceededException() {
        return new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
    }

    private class DrainRunnable implements Runnable {

        @Override
        public void run() {
            Subscription s = subscription;
            if (s != null && status == Status.RUNNING) {
                REQUESTED.decrementAndGet(RateLimiterSubscriber.this);
                s.request(1);
            }
        }
    }

    private enum Status {
        RUNNING,
        DONE,
        REJECTED
    }
}
