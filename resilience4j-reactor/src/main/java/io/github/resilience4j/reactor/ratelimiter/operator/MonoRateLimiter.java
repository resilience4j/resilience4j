package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.scheduler.Scheduler;

public class MonoRateLimiter<T> extends MonoOperator<T, T> {
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;

    public MonoRateLimiter(Mono<? extends T> source, RateLimiter rateLimiter,
                           Scheduler scheduler) {
        super(source);
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        source.publishOn(scheduler)
                .subscribe(new RateLimiterSubscriber<>(rateLimiter, actual));
    }
}
