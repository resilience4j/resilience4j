package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.monitoring.health.RateLimitersHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({RateLimiter.class, HealthIndicator.class, StatusAggregator.class})
@AutoConfigureAfter(RateLimiterAutoConfiguration.class)
@AutoConfigureBefore(HealthContributorAutoConfiguration.class)
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
