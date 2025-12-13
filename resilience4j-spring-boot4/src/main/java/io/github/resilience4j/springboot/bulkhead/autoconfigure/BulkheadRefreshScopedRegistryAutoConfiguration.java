package io.github.resilience4j.springboot.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.bulkhead.configure.BulkheadConfiguration;
import io.github.resilience4j.spring6.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.spring6.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = BulkheadAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@ConditionalOnClass({Bulkhead.class, RefreshScope.class})
@EnableConfigurationProperties({BulkheadProperties.class, ThreadPoolBulkheadProperties.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class BulkheadRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
    private final ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();

    /**
     * Overriding {@link BulkheadAutoConfiguration#bulkheadRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public BulkheadRegistry bulkheadRegistry(
        BulkheadConfigurationProperties bulkheadConfigurationProperties,
        EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
        RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer,
        @Qualifier("compositeBulkheadCustomizer") CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer) {
        return bulkheadConfiguration
            .bulkheadRegistry(bulkheadConfigurationProperties, bulkheadEventConsumerRegistry,
                bulkheadRegistryEventConsumer, compositeBulkheadCustomizer);
    }

    /**
     * Overriding {@link BulkheadAutoConfiguration#threadPoolBulkheadRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(
        CommonThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties,
        EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
        RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer,
        @Qualifier("compositeThreadPoolBulkheadCustomizer") CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer) {
        return threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(
            threadPoolBulkheadConfigurationProperties, bulkheadEventConsumerRegistry,
            threadPoolBulkheadRegistryEventConsumer, compositeThreadPoolBulkheadCustomizer);
    }
}
