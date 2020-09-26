package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry;

import java.util.List;
import java.util.Map;

interface RateLimiterRegistryFactory<C extends RateLimiterConfig> {

    RateLimiterRegistry create(Map<String, C> configs,
                               List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                               io.vavr.collection.Map<String, String> tags, RegistryStore<RateLimiter> registryStore);

    static RateLimiterRegistryFactory<RateLimiterConfig> defaultConfig() {
        return (configs, registryEventConsumers, tags, registryStore) -> InMemoryRateLimiterRegistry.create(configs,registryEventConsumers,tags, registryStore);
    }

    static RateLimiterRegistryFactory<RefillRateLimiterConfig> refillConfig() {
        return (configs, registryEventConsumers, tags, registryStore) -> InMemoryRateLimiterRegistry.createRefill(configs,registryEventConsumers,tags, registryStore);
    }

}
