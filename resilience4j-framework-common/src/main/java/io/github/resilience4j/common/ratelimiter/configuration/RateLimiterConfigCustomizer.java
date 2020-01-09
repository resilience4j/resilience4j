package io.github.resilience4j.common.ratelimiter.configuration;

import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

/**
 * Enable customization rate limiter configuration builders programmatically.
 */
public interface RateLimiterConfigCustomizer extends CustomizerWithName {

    /**
     * Customize rate limiter configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(RateLimiterConfig.Builder configBuilder);

}
