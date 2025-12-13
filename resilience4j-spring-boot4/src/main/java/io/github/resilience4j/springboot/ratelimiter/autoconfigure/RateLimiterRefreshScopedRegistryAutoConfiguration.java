package io.github.resilience4j.springboot.ratelimiter.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = RateLimiterAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@ConditionalOnClass({RateLimiter.class, RefreshScope.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class RateLimiterRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();

    /**
     * Overriding {@link RateLimiterAutoConfiguration#rateLimiterRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(
            RateLimiterConfigurationProperties rateLimiterProperties,
            EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
            RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer,
            @Qualifier("compositeRateLimiterCustomizer") CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {
        return rateLimiterConfiguration
                .rateLimiterRegistry(rateLimiterProperties, rateLimiterEventsConsumerRegistry,
                        rateLimiterRegistryEventConsumer, compositeRateLimiterCustomizer);
    }
}
