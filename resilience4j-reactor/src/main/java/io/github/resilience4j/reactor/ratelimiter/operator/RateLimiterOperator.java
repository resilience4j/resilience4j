package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.FluxResilience;
import io.github.resilience4j.reactor.MonoResilience;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;


/**
 * A Reactor operator which wraps a reactive type in a rate limiter.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class RateLimiterOperator<T> implements Function<Publisher<T>, Publisher<T>> {
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;

    private RateLimiterOperator(RateLimiter rateLimiter, Scheduler scheduler) {
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param rateLimiter the Rate limiter
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter) {
        return of(rateLimiter, Schedulers.parallel());
    }

    /**
     * Creates a RateLimiterOperator.
     *
     * @param <T>         the value type of the upstream and downstream
     * @param rateLimiter the Rate limiter
     * @param scheduler   the {@link Scheduler} where to publish
     * @return a RateLimiterOperator
     */
    public static <T> RateLimiterOperator<T> of(RateLimiter rateLimiter, Scheduler scheduler) {
        return new RateLimiterOperator<>(rateLimiter, scheduler);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return MonoResilience
                    .onAssembly(new MonoRateLimiter<T>((Mono<? extends T>) publisher, rateLimiter, scheduler));
        } else if (publisher instanceof Flux) {
            return FluxResilience
                    .onAssembly(new FluxRateLimiter<T>((Flux<? extends T>) publisher, rateLimiter, scheduler));
        }

        throw new IllegalStateException("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> are not supported by this operator");
    }
}
