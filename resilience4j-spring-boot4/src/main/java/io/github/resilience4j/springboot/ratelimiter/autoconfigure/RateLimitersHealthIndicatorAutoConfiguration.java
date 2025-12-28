package io.github.resilience4j.springboot.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.springboot.ratelimiter.monitoring.health.RateLimitersHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = HealthContributorAutoConfiguration.class, after = RateLimiterAutoConfiguration.class)
@ConditionalOnClass({RateLimiter.class, HealthIndicator.class, HealthContributorAutoConfiguration.class, StatusAggregator.class})
public class RateLimitersHealthIndicatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "rateLimitersHealthIndicator")
    @ConditionalOnProperty(prefix = "management.health.ratelimiters", name = "enabled")
    public RateLimitersHealthIndicator rateLimitersHealthIndicator(
        RateLimiterRegistry rateLimiterRegistry,
        RateLimiterConfigurationProperties rateLimiterProperties,
        StatusAggregator statusAggregator) {
        return new RateLimitersHealthIndicator(rateLimiterRegistry, rateLimiterProperties, statusAggregator);
    }

}
