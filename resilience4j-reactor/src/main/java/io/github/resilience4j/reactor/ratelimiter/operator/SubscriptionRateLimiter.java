package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;

class SubscriptionRateLimiter<T> {

    private static Subscription NO_OP_SUBSCRIPTION = new NoOpSubscription();
    private static int PERMISSION_AVAILABLE = 0;

    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Publisher<? extends T> source;

    SubscriptionRateLimiter(Publisher<? extends T> source, RateLimiter rateLimiter, Scheduler scheduler) {
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.source = source;
    }

    public void subscribe(CoreSubscriber<? super T> actual) {
        final long wait = rateLimiter.reservePermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration());
        if (wait < 0) {
            actual.onSubscribe(NO_OP_SUBSCRIPTION);
            actual.onError(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
            return;
        }

        if (wait == PERMISSION_AVAILABLE) {
            source.subscribe(actual);
        } else {
            Mono.delay(Duration.ofNanos(wait), scheduler)
                    .subscribe(aLong -> source.subscribe(actual));
        }
    }

    private static class NoOpSubscription implements Subscription {
        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }
    }
}
