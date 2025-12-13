package io.github.resilience4j.springboot.timelimiter.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfiguration;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = TimeLimiterAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@ConditionalOnClass({TimeLimiter.class, RefreshScope.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class TimeLimiterRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    protected final TimeLimiterConfiguration timeLimiterConfiguration = new TimeLimiterConfiguration();

    /**
     * Overriding {@link TimeLimiterAutoConfiguration#timeLimiterRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public TimeLimiterRegistry timeLimiterRegistry(
            TimeLimiterConfigurationProperties timeLimiterProperties,
            EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry,
            RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
            @Qualifier("compositeTimeLimiterCustomizer") CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        return timeLimiterConfiguration.timeLimiterRegistry(
                timeLimiterProperties, timeLimiterEventsConsumerRegistry,
                timeLimiterRegistryEventConsumer, compositeTimeLimiterCustomizer);
    }
}
