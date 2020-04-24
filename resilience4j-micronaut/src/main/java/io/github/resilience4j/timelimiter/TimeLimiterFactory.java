package io.github.resilience4j.timelimiter;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vavr.collection.HashMap;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Factory
public class TimeLimiterFactory {

    @Bean
    @Named("compositeTimeLimiterCustomizer")
    public CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<TimeLimiterConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    public TimeLimiterRegistry timeLimiterRegistry(
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
        EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventConsumerRegistry,
        RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        @Named("compositeTimeLimiterCustomizer") CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        TimeLimiterRegistry timeLimiterRegistry = createTimeLimiterRegistry(timeLimiterConfigurationProperties, timeLimiterRegistryEventConsumer, compositeTimeLimiterCustomizer);

        return timeLimiterRegistry;
    }

    /**
     * Initializes a timeLimiter registry.
     *
     * @param timeLimiterConfigurationProperties The timeLimiter configuration properties.
     * @return a timeLimiterRegistry
     */
    private static TimeLimiterRegistry createTimeLimiterRegistry(
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
        RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {

        Map<String, TimeLimiterConfig> configs = timeLimiterConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> timeLimiterConfigurationProperties.createTimeLimiterConfig(
                    entry.getKey(), entry.getValue(), compositeTimeLimiterCustomizer)));

        return TimeLimiterRegistry.of(configs, timeLimiterRegistryEventConsumer,
            HashMap.ofAll(timeLimiterConfigurationProperties.getTags()));
    }
}
