package io.github.resilience4j.timelimiter;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.vavr.collection.HashMap;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Factory
@Requires(property = "resilience4j.timelimiter.enabled")
public class TimeLimiterRegistryFactory {

    @Bean
    @Named("compositeTimeLimiterCustomizer")
    public CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<TimeLimiterConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    @Requires(beans = TimeLimiterConfigurationProperties.class)
    public TimeLimiterRegistry timeLimiterRegistry(
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
        EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventConsumerRegistry,
        RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        @Named("compositeTimeLimiterCustomizer") CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        TimeLimiterRegistry timeLimiterRegistry =
            createTimeLimiterRegistry(timeLimiterConfigurationProperties, timeLimiterRegistryEventConsumer,
                compositeTimeLimiterCustomizer);
        registerEventConsumer(timeLimiterRegistry, timeLimiterEventConsumerRegistry, timeLimiterConfigurationProperties);

        initTimeLimiterRegistry(timeLimiterRegistry, timeLimiterConfigurationProperties, compositeTimeLimiterCustomizer);
        return timeLimiterRegistry;
    }


    @Bean
    @Primary
    public RegistryEventConsumer<TimeLimiterEvent> timeLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<TimeLimiterEvent>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    @Bean
    public EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    void initTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry, TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
                                 CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        timeLimiterConfigurationProperties.getInstances().forEach(
            (name, properties) -> timeLimiterRegistry.timeLimiter(name,
                timeLimiterConfigurationProperties.createTimeLimiterConfig(name, properties, compositeTimeLimiterCustomizer))
        );
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

    /**
     * Registers the post creation consumer function that registers the consumer events to the timeLimiters.
     *
     * @param timeLimiterRegistry                The timeLimiter registry.
     * @param eventConsumerRegistry              The event consumer registry.
     * @param timeLimiterConfigurationProperties timeLimiter configuration properties
     */
    private static void registerEventConsumer(TimeLimiterRegistry timeLimiterRegistry,
                                              EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry,
                                              TimeLimiterConfigurationProperties timeLimiterConfigurationProperties) {
        timeLimiterRegistry.getEventPublisher().onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), timeLimiterConfigurationProperties));
    }

    private static void registerEventConsumer(EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry, TimeLimiter timeLimiter,
                                              TimeLimiterConfigurationProperties timeLimiterConfigurationProperties) {
        int eventConsumerBufferSize = Optional.ofNullable(timeLimiterConfigurationProperties.getInstanceProperties(timeLimiter.getName()))
            .map(io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        timeLimiter.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(timeLimiter.getName(), eventConsumerBufferSize));
    }

}
