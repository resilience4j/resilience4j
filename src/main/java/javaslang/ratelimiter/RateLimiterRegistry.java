package javaslang.ratelimiter;

import javaslang.ratelimiter.internal.InMemoryRateLimiterRegistry;

import java.util.function.Supplier;

/**
 * Manages all RateLimiter instances.
 */
public interface RateLimiterRegistry {

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RateLimiterConfig rateLimiterConfig);

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a custom RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier);

    static RateLimiterRegistry of(RateLimiterConfig defaultRateLimiterConfig) {
        return new InMemoryRateLimiterRegistry(defaultRateLimiterConfig);
    }
}
