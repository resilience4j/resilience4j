package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of circuit breaker registry
 */
@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerConfigurationTest {

    @Test
    public void testCreateCircuitBreakerRegistry() {
        InstanceProperties instanceProperties1 = new InstanceProperties();
        instanceProperties1.setSlidingWindowSize(1000);
        InstanceProperties instanceProperties2 = new InstanceProperties();
        instanceProperties2.setSlidingWindowSize(1337);
        CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.setCircuitBreakerAspectOrder(400);
        circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerConfigurationProperties);
        DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                compositeCircuitBreakerCustomizerTestInstance());

        assertThat(circuitBreakerConfigurationProperties.getCircuitBreakerAspectOrder())
            .isEqualTo(400);
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);
        CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backend1");
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(1000);
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backend2");
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(1337);
        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
    }

    @Test
    public void testCreateCircuitBreakerRegistryWithSharedConfigs() {
        InstanceProperties defaultProperties = new InstanceProperties();
        defaultProperties.setSlidingWindowSize(1000);
        defaultProperties.setPermittedNumberOfCallsInHalfOpenState(100);
        InstanceProperties sharedProperties = new InstanceProperties();
        sharedProperties.setSlidingWindowSize(1337);
        sharedProperties.setPermittedNumberOfCallsInHalfOpenState(1000);
        InstanceProperties backendWithDefaultConfig = new InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setPermittedNumberOfCallsInHalfOpenState(99);
        InstanceProperties backendWithSharedConfig = new InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setPermittedNumberOfCallsInHalfOpenState(999);
        CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
        circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);
        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);
        CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerConfigurationProperties);
        DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                compositeCircuitBreakerCustomizerTestInstance());

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);
        // Should get default config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreaker circuitBreaker1 = circuitBreakerRegistry
            .circuitBreaker("backendWithDefaultConfig");
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(1000);
        assertThat(
            circuitBreaker1.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(99);
        // Should get shared config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry
            .circuitBreaker("backendWithSharedConfig");
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(1337);
        assertThat(
            circuitBreaker2.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(999);
        // Unknown backend should get default config of Registry
        CircuitBreaker circuitBreaker3 = circuitBreakerRegistry.circuitBreaker("unknownBackend");
        assertThat(circuitBreaker3).isNotNull();
        assertThat(circuitBreaker3.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(1000);
        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
    }

    @Test
    public void testCreateCircuitBreakerRegistryWithUnknownConfig() {
        CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
        InstanceProperties instanceProperties = new InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        circuitBreakerConfigurationProperties.getInstances().put("backend", instanceProperties);
        CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerConfigurationProperties);
        DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        assertThatThrownBy(() -> circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                compositeCircuitBreakerCustomizerTestInstance()))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

    private CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizerTestInstance() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }

}