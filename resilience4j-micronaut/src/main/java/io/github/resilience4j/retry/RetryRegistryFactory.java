package io.github.resilience4j.retry;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.event.RetryEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Factory
@Requires(property = "resilience4j.retry.enabled")
public class RetryRegistryFactory {

    @Bean
    @Named("compositeRetryCustomizer")
    public CompositeCustomizer<RetryConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<RetryConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    @Requires(beans = RetryConfigurationProperties.class)
    public RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryConfigurationProperties,
        EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
        RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        @Named("compositeTimeLimiterCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        RetryRegistry retryRegistry = createRetryRegistry(retryConfigurationProperties,
            retryRegistryEventConsumer, compositeRetryCustomizer);
        registerEventConsumer(retryRegistry, retryEventConsumerRegistry,
            retryConfigurationProperties);
        retryConfigurationProperties.getInstances()
            .forEach((name, properties) ->
                retryRegistry.retry(name, retryConfigurationProperties
                    .createRetryConfig(name, compositeRetryCustomizer)));
        return retryRegistry;
    }


    @Bean
    public EventConsumerRegistry<RetryEvent> rateLimiterEventEventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Retry> rateLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Retry>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the rate
     * limiters.
     *
     * @param retryRegistry         The rate limiter registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(RetryRegistry retryRegistry,
                                       EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
                                       RetryConfigurationProperties rateLimiterConfigurationProperties) {
        retryRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(),
                rateLimiterConfigurationProperties));
    }

    private void registerEventConsumer(
        EventConsumerRegistry<RetryEvent> eventConsumerRegistry, Retry retry,
        RetryConfigurationProperties retryConfigurationProperties) {
        int eventConsumerBufferSize = Optional.ofNullable(retryConfigurationProperties.getBackendProperties(retry.getName()))
            .map(RetryConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        retry.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(retry.getName(), eventConsumerBufferSize));
    }


    /**
     * Initializes a rate limiter registry.
     *
     * @param retryProperties                The rate limiter configuration properties.
     * @param compositeRateLimiterCustomizer the composite rate limiter customizer delegate
     * @return a RateLimiterRegistry
     */
    private RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryProperties,
        RegistryEventConsumer<Retry> rateLimiterRegistryEventConsumer,
        CompositeCustomizer<RetryConfigCustomizer> compositeRateLimiterCustomizer) {
        Map<String, RetryConfig> configs = retryProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> retryProperties
                    .createRetryConfig(entry.getValue(), compositeRateLimiterCustomizer,
                        entry.getKey())));

        return RetryRegistry.of(configs, rateLimiterRegistryEventConsumer,
            io.vavr.collection.HashMap.ofAll(retryProperties.getTags()));
    }
}
