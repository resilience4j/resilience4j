package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of circuit breaker registry
 */
@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerConfigurationTest {

	@Test
    @SuppressWarnings("deprecation") // Left this use for testing purposes
	public void testCreateCircuitBreakerRegistryUsingDeprecatedOptions() {
		//Given
		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties1.setRingBufferSizeInClosedState(1000);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties2.setRingBufferSizeInClosedState(1337);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.setCircuitBreakerAspectOrder(400);
		circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
		circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

		//Then
		assertThat(circuitBreakerConfigurationProperties.getCircuitBreakerAspectOrder()).isEqualTo(400);
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);
		CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backend1");
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);

		CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backend2");
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1337);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateCircuitBreakerRegistry() {
		//Given
		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties1.setSlidingWindowSize(1000);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties2.setSlidingWindowSize(1337);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.setCircuitBreakerAspectOrder(400);
		circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
		circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

		//Then
		assertThat(circuitBreakerConfigurationProperties.getCircuitBreakerAspectOrder()).isEqualTo(400);
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);
		CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backend1");
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);

		CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backend2");
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1337);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
    @SuppressWarnings("deprecation") // Left this use for testing purposes
	public void testCreateCircuitBreakerRegistryWithSharedConfigsUsingDeprecatedOptions() {
		//Given
		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		defaultProperties.setRingBufferSizeInClosedState(1000);
		defaultProperties.setRingBufferSizeInHalfOpenState(100);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		sharedProperties.setRingBufferSizeInClosedState(1337);
		sharedProperties.setRingBufferSizeInHalfOpenState(1000);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setRingBufferSizeInHalfOpenState(99);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setRingBufferSizeInHalfOpenState(999);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
		circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		circuitBreakerConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
		circuitBreakerConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

		//Then
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);

		// Should get default config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backendWithDefaultConfig");
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(99);

		// Should get shared config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backendWithSharedConfig");
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1337);
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(999);

		// Unknown backend should get default config of Registry
		CircuitBreaker circuitBreaker3 = circuitBreakerRegistry.circuitBreaker("unknownBackend");
		assertThat(circuitBreaker3).isNotNull();
		assertThat(circuitBreaker3.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}

    @Test
    public void testCreateCircuitBreakerRegistryWithSharedConfigs() {
        //Given
        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlidingWindowSize(1000);
        defaultProperties.setPermittedNumberOfCallsInHalfOpenState(100);

        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        sharedProperties.setSlidingWindowSize(1337);
        sharedProperties.setPermittedNumberOfCallsInHalfOpenState(1000);

        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setPermittedNumberOfCallsInHalfOpenState(99);

        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setPermittedNumberOfCallsInHalfOpenState(999);

        CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
        circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        circuitBreakerConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
        circuitBreakerConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

        CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
        DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

        //Then
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);

        // Should get default config and overwrite setRingBufferSizeInHalfOpenState
        CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backendWithDefaultConfig");
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);
        assertThat(circuitBreaker1.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(99);

        // Should get shared config and overwrite setRingBufferSizeInHalfOpenState
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backendWithSharedConfig");
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1337);
        assertThat(circuitBreaker2.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(999);

        // Unknown backend should get default config of Registry
        CircuitBreaker circuitBreaker3 = circuitBreakerRegistry.circuitBreaker("unknownBackend");
        assertThat(circuitBreaker3).isNotNull();
        assertThat(circuitBreaker3.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(1000);

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
    }

	@Test
	public void testCreateCircuitBreakerRegistryWithUnknownConfig() {
		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties.setBaseConfig("unknownConfig");
		circuitBreakerConfigurationProperties.getInstances().put("backend", instanceProperties);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList())))
			.isInstanceOf(ConfigurationNotFoundException.class)
			.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}