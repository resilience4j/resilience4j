package io.github.resilience4j.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadConfiguration;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractRefreshScopedBulkheadConfiguration {

    protected final BulkheadConfiguration bulkheadConfiguration;
    protected final ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration;

    protected AbstractRefreshScopedBulkheadConfiguration() {
        this.threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
        this.bulkheadConfiguration = new BulkheadConfiguration();
    }

    /**
     * @param bulkheadConfigurationProperties bulkhead spring configuration properties
     * @param bulkheadEventConsumerRegistry   the bulkhead event consumer registry
     * @return the RefreshScoped BulkheadRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public BulkheadRegistry bulkheadRegistry(BulkheadConfigurationProperties bulkheadConfigurationProperties,
                                             EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry) {
        return bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, bulkheadEventConsumerRegistry);
    }

    /**
     * @param threadPoolBulkheadConfigurationProperties thread pool bulkhead spring configuration properties
     * @param bulkheadEventConsumerRegistry   the bulk head event consumer registry
     * @return the RefreshScoped ThreadPoolBulkheadRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties,
                                                                 EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry) {

        return threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(threadPoolBulkheadConfigurationProperties, bulkheadEventConsumerRegistry);
    }

}
