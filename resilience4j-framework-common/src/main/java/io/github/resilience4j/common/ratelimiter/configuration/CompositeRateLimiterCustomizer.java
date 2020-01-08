package io.github.resilience4j.common.ratelimiter.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * the composite  of any rate limiter {@link RateLimiterConfigCustomizer} implementations.
 */
public class CompositeRateLimiterCustomizer {

    private final Map<String, RateLimiterConfigCustomizer> customizerMap = new HashMap<>();

    public CompositeRateLimiterCustomizer(List<RateLimiterConfigCustomizer> customizers) {
        if (customizers != null && !customizers.isEmpty()) {
            customizerMap.putAll(customizers.stream()
                .collect(
                    Collectors.toMap(RateLimiterConfigCustomizer::name, Function.identity())));
        }
    }

    /**
     * @param rateLimiterInstanceName the rate limiter instance name
     * @return the found {@link RateLimiterConfigCustomizer} if any .
     */
    public Optional<RateLimiterConfigCustomizer> getRateLimiterConfigCustomizer(
        String rateLimiterInstanceName) {
        return Optional.ofNullable(customizerMap.get(rateLimiterInstanceName));
    }

}
