package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Factory
public class RateLimiterRegistryFactory {


    @Factory
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterProperties rateLimiterProperties) {
        RateLimiterRegistry rateLimiterRegistry = createRateLimiterRegistry(rateLimiterProperties, null, null);

        return rateLimiterRegistry;
    }

    @Bean
    @Primary
    public RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<RateLimiter>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    private RateLimiterRegistry createRateLimiterRegistry(RateLimiterProperties rateLimiterProperties,
                                                  RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer,
                                                  CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {

        Map<String, RateLimiterConfig> configs = rateLimiterProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> rateLimiterProperties.createRateLimiterConfig(entry.getValue(), new CompositeCustomizer<>(Collections.emptyList()),
                    entry.getKey())));

        return RateLimiterRegistry.of(configs, rateLimiterRegistryEventConsumer,
            io.vavr.collection.HashMap.ofAll(rateLimiterProperties.getTags()));
    }



    private RateLimiterRegistry createRateLimiterRegistery(

    )

}
