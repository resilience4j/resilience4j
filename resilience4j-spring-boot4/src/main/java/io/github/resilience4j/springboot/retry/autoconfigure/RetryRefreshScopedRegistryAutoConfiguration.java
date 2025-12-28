package io.github.resilience4j.springboot.retry.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.spring6.retry.configure.RetryConfiguration;
import io.github.resilience4j.spring6.retry.configure.RetryConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = RetryAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@ConditionalOnClass({Retry.class, RefreshScope.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class RetryRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final RetryConfiguration retryConfiguration = new RetryConfiguration();

    /**
     * Overriding {@link RetryAutoConfiguration#retryRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(
            RetryConfigurationProperties retryConfigurationProperties,
            EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
            RegistryEventConsumer<Retry> retryRegistryEventConsumer,
            @Qualifier("compositeRetryCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return retryConfiguration.retryRegistry(retryConfigurationProperties, retryEventConsumerRegistry,
                retryRegistryEventConsumer, compositeRetryCustomizer);
    }
}
