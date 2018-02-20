package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.scheduler.Scheduler;

public class FluxRateLimiter<T> extends FluxOperator<T, T> {

    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;

    public FluxRateLimiter(Flux<? extends T> source, RateLimiter rateLimiter,
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