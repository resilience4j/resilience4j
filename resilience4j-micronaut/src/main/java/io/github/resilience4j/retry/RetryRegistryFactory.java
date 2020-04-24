package io.github.resilience4j.retry;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Factory
public class RetryRegistryFactory {

    @Bean
    @Named("compositeRetryCustomizer")
    public CompositeCustomizer<RetryConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<RetryConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }


    @Singleton
    public RetryRegistry retryRegistry(RetryProperties retryProperties) {
        return null;
    }


    /**
     * Initializes a rate limiter registry.
     *
     * @param rateLimiterConfigurationProperties The rate limiter configuration properties.
     * @param compositeRateLimiterCustomizer     the composite rate limiter customizer delegate
     * @return a RateLimiterRegistry
     */
    private RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryConfigurationProperties,
        RegistryEventConsumer<Retry> rateLimiterRegistryEventConsumer,
        CompositeCustomizer<RetryConfigCustomizer> compositeRateLimiterCustomizer) {
        Map<String, RetryConfig> configs = retryConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> retryConfigurationProperties
                    .createRetryConfig(entry.getValue(), compositeRateLimiterCustomizer,
                        entry.getKey())));

        return RetryRegistry.of(configs, rateLimiterRegistryEventConsumer,
            io.vavr.collection.HashMap.ofAll(retryConfigurationProperties.getTags()));
    }
}
