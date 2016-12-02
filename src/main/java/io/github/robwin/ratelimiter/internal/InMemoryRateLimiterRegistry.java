package io.github.robwin.ratelimiter.internal;

import static java.util.Objects.requireNonNull;

import io.github.robwin.ratelimiter.RateLimiter;
import io.github.robwin.ratelimiter.RateLimiterConfig;
import io.github.robwin.ratelimiter.RateLimiterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Backend RateLimiter manager.
 * Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRateLimiterRegistry implements RateLimiterRegistry {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";

    private final RateLimiterConfig defaultRateLimiterConfig;
    /**
     * The RateLimiters, indexed by name of the backend.
     */
    private final Map<String, RateLimiter> rateLimiters;

    public InMemoryRateLimiterRegistry(final RateLimiterConfig defaultRateLimiterConfig) {
        this.defaultRateLimiterConfig = requireNonNull(defaultRateLimiterConfig, CONFIG_MUST_NOT_BE_NULL);
        rateLimiters = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name) {
        return rateLimiter(name, defaultRateLimiterConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name, final RateLimiterConfig rateLimiterConfig) {
        requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL);
        return rateLimiters.computeIfAbsent(
            name,
            limitName -> new SemaphoreBasedRateLimiter(name, rateLimiterConfig)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name, final Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
        requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        requireNonNull(rateLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL);
        return rateLimiters.computeIfAbsent(
            name,
            limitName -> {
                RateLimiterConfig rateLimiterConfig = rateLimiterConfigSupplier.get();
                requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL);
                return new SemaphoreBasedRateLimiter(limitName, rateLimiterConfig);
            }
        );
    }
}
