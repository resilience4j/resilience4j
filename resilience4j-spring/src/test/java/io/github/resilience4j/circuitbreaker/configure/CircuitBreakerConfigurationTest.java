package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of circuit breaker registry
 */
@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerConfigurationTest {

	@Test
	public void testCreateCircuitBreakerRegistry() {
		//Given
		CircuitBreakerConfigurationProperties.BackendProperties backendProperties1 = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendProperties1.setRingBufferSizeInClosedState(1000);

		CircuitBreakerConfigurationProperties.BackendProperties backendProperties2 = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendProperties2.setRingBufferSizeInClosedState(1337);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.getBackends().put("backend1", backendProperties1);
		circuitBreakerConfigurationProperties.getBackends().put("backend2", backendProperties2);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry);

		//Then
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);
		CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backend1");
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(1000);

		CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backend2");
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(1337);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateCircuitBreakerRegistryWithSharedConfigs() {
		//Given
		CircuitBreakerConfigurationProperties.BackendProperties defaultProperties = new CircuitBreakerConfigurationProperties.BackendProperties();
		defaultProperties.setRingBufferSizeInClosedState(1000);
		defaultProperties.setRingBufferSizeInHalfOpenState(100);

		CircuitBreakerConfigurationProperties.BackendProperties sharedProperties = new CircuitBreakerConfigurationProperties.BackendProperties();
		sharedProperties.setRingBufferSizeInClosedState(1337);
		sharedProperties.setRingBufferSizeInHalfOpenState(1000);

		CircuitBreakerConfigurationProperties.BackendProperties backendWithDefaultConfig = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setRingBufferSizeInHalfOpenState(99);

		CircuitBreakerConfigurationProperties.BackendProperties backendWithSharedConfig = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setRingBufferSizeInHalfOpenState(999);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
		circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		circuitBreakerConfigurationProperties.getBackends().put("backendWithDefaultConfig", backendWithDefaultConfig);
		circuitBreakerConfigurationProperties.getBackends().put("backendWithSharedConfig", backendWithSharedConfig);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry);

		//Then
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(2);

		// Should get default config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker("backendWithDefaultConfig");
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(1000);
		assertThat(circuitBreaker1.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(99);

		// Should get shared config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("backendWithSharedConfig");
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(1337);
		assertThat(circuitBreaker2.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(999);

		// Unknown backend should get default config of Registry
		CircuitBreaker circuitBreaker3 = circuitBreakerRegistry.circuitBreaker("unknownBackend");
		assertThat(circuitBreaker3).isNotNull();
		assertThat(circuitBreaker3.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(1000);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}

	@Test
	public void testCreateCircuitBreakerRegistryWithUnknownConfig() {
		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();

		CircuitBreakerConfigurationProperties.BackendProperties backendProperties = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendProperties.setBaseConfig("unknownConfig");
		circuitBreakerConfigurationProperties.getBackends().put("backend", backendProperties);

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		DefaultEventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> circuitBreakerConfiguration.circuitBreakerRegistry(eventConsumerRegistry))
			.isInstanceOf(ConfigurationNotFoundException.class)
			.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}