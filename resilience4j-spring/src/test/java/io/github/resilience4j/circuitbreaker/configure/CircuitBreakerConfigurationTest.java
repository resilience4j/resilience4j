package io.github.resilience4j.circuitbreaker.configure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;

/**
 * test custom init of circuit breaker registry
 */
@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerConfigurationTest {

	@Mock
	private CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties;


	@Test
	public void testCircuitBreakerRegistryConfig() {
		CircuitBreakerConfigurationProperties.BackendProperties backendProperties = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendProperties.setFailureRateThreshold(3);
		when(circuitBreakerConfigurationProperties.getBackends()).thenReturn(Collections.singletonMap("testBackend", backendProperties));
		when(circuitBreakerConfigurationProperties.createCircuitBreakerConfig(anyString())).thenReturn(CircuitBreakerConfig.ofDefaults());

		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(new DefaultEventConsumerRegistry());
		assertThat(circuitBreakerRegistry.getAllCircuitBreakers().size()).isEqualTo(1);
		assertThat(circuitBreakerRegistry.circuitBreaker("testBackend")).isNotNull();

	}

	@Test
	public void testCircuitBreakerSharedFindConfig() {
		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		CircuitBreakerConfigurationProperties.BackendProperties backendPropertiesShared = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendPropertiesShared.setFailureRateThreshold(3);
		backendPropertiesShared.setSharedConfigName("sharedConfigDefault");
		circuitBreakerConfigurationProperties.createCircuitBreakerConfigFromShared("sharedConfig");
		/*when(circuitBreakerConfigurationProperties.getBackends()).thenReturn(Collections.emptyMap());
		when(circuitBreakerConfigurationProperties.getSharedConfigs()).thenReturn(Collections.singletonMap("sharedConfig", backendPropertiesShared));
		when(circuitBreakerConfigurationProperties.findCircuitBreakerBackend(any(),any())).thenCallRealMethod();*/
		CircuitBreakerConfiguration circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerConfigurationProperties);
		CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.circuitBreakerRegistry(new DefaultEventConsumerRegistry());

		assertThat(circuitBreakerConfigurationProperties.findCircuitBreakerBackend(circuitBreakerRegistry.circuitBreaker("sharedConfig"), CircuitBreakerConfig.custom()
				.configurationName("sharedConfigDefault").build())).isNotNull();

	}

	@Test
	public void testCircuitBreakerSharedConfig() {
		CircuitBreakerConfigurationProperties properties = new CircuitBreakerConfigurationProperties();

		assertThat(properties.createCircuitBreakerConfig("backend")).isNotNull();
		assertThat(properties.createCircuitBreakerConfigFromShared("sharedConfig")).isNotNull();
		assertThat(properties.getBackends().size()).isEqualTo(0);
		assertThat(properties.getSharedConfigs().size()).isEqualTo(0);

	}

}