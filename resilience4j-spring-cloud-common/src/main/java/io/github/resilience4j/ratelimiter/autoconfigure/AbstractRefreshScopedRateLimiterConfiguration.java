package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractRefreshScopedRateLimiterConfiguration {

    protected final RateLimiterConfiguration rateLimiterConfiguration;

    protected AbstractRefreshScopedRateLimiterConfiguration() {
        this.rateLimiterConfiguration = new RateLimiterConfiguration();
    }

    /**
     * @param rateLimiterProperties ratelimiter spring configuration properties
     * @param rateLimiterEventsConsumerRegistry the ratelimiter event consumer registry
     * @return the RefreshScoped RateLimiterRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterConfigurationProperties rateLimiterProperties,
                                                   EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                   RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer) {
        return rateLimiterConfiguration.rateLimiterRegistry(
                rateLimiterProperties, rateLimiterEventsConsumerRegistry, rateLimiterRegistryEventConsumer);
    }

}
