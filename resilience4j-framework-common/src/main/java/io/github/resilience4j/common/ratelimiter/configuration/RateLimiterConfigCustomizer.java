package io.github.resilience4j.common.ratelimiter.configuration;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;

/**
 * Enable customization rate limiter configuration builders programmatically.
 */
public interface RateLimiterConfigCustomizer {

    /**
     * Customize rate limiter configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(RateLimiterConfig.Builder configBuilder);

    /**
     * @return name of the rate limiter instance to be customized
     */
    String name();
}
