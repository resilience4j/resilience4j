package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.internal.PermittedOperator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import static java.util.Objects.requireNonNull;

/**
 * A disposable rate limiter acting as a base class for rate limiter operators.
 *
 * @param <T> the type of the emitted event
 * @param <D> the actual type of the disposable/subscription
 */
abstract class AbstractRateLimiterOperator<T, D> extends PermittedOperator<T, D> {
    private final RateLimiter rateLimiter;

    AbstractRateLimiterOperator(RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
    }

    @Override
    protected boolean tryCallPermit() {
        return rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }

    @Override
    protected Exception notPermittedException() {
        return new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
    }
}
