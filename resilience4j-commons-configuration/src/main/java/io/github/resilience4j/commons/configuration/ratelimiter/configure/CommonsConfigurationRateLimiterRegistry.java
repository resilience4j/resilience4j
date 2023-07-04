package io.github.resilience4j.commons.configuration.ratelimiter.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationRateLimiterRegistry {
    private CommonsConfigurationRateLimiterRegistry() {
    }

    /**
     * Create a RateLimiterRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for rate limiter configuration
     * @return a RateLimiterRegistry with a Map of shared RateLimiter configurations.
     */
    public static RateLimiterRegistry of(Configuration configuration, CompositeCustomizer<RateLimiterConfigCustomizer> customizer){
        CommonRateLimiterConfigurationProperties rateLimiterProperties = CommonsConfigurationRateLimiterConfiguration.of(configuration);
        Map<String, RateLimiterConfig> rateLimiterConfigMap = rateLimiterProperties.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> rateLimiterProperties.createRateLimiterConfig(entry.getValue(), customizer, entry.getKey())));
        return RateLimiterRegistry.of(rateLimiterConfigMap);
    }
}
