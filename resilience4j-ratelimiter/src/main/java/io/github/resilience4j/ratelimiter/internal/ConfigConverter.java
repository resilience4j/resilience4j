package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;

/**
 * The purpose of config converter is to make sure
 */
interface ConfigConverter<C extends RateLimiterConfig> {

    C from(RateLimiterConfig config);

    RateLimiterConfig to(C config);

    static ConfigConverter<RateLimiterConfig> defaultConverter() {
        return new ConfigConverter<RateLimiterConfig>() {
            @Override
            public RateLimiterConfig from(RateLimiterConfig config) {
                return config;
            }

            @Override
            public RateLimiterConfig to(RateLimiterConfig config) {
                return config;
            }
        };
    }

    static ConfigConverter<RefillRateLimiterConfig> refillConverter() {

        return new ConfigConverter<RefillRateLimiterConfig>() {
            @Override
            public RefillRateLimiterConfig from(RateLimiterConfig config) {
                return null;
                //TODO return new RefillRateLimiterConfig(config);
            }

            @Override
            public RateLimiterConfig to(RefillRateLimiterConfig config) {
                return config;
            }
        };
    }

}
