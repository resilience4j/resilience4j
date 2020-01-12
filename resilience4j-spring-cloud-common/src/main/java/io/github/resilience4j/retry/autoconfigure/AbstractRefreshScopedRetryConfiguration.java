package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.configure.RetryConfiguration;
import io.github.resilience4j.retry.configure.RetryConfigurationProperties;
import io.github.resilience4j.retry.event.RetryEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractRefreshScopedRetryConfiguration {

    protected final RetryConfiguration retryConfiguration;

    protected AbstractRefreshScopedRetryConfiguration() {
        this.retryConfiguration = new RetryConfiguration();
    }

    /**
     * @param retryConfigurationProperties retry spring configuration properties
     * @param retryEventConsumerRegistry   the retry event consumer registry
     * @return the RefreshScoped RetryRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties,
        EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
        RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        @Qualifier("compositeRetryCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return retryConfiguration
            .retryRegistry(retryConfigurationProperties, retryEventConsumerRegistry,
                retryRegistryEventConsumer, compositeRetryCustomizer);
    }

}
