package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;


public class InMemoryCircuitBreakerRegistryTest {

	private Logger LOGGER;

	@Before
	public void setUp() {
		LOGGER = mock(Logger.class);
	}

	@Test
	public void testAddCircuitBreakerRegistry() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());
		assertThat(circuitBreakerRegistry.getConfiguration("testConfig")).isNotNull();
	}

	@Test
	public void testGetNotFoundCircuitBreakerRegistry() {
		InMemoryCircuitBreakerRegistry circuitBreakerRegistry = (InMemoryCircuitBreakerRegistry) CircuitBreakerRegistry.ofDefaults();
		assertThat(circuitBreakerRegistry.getConfiguration("testNotFound")).isEmpty();
	}

	@Test
	public void testUpdateDefaultCircuitBreakerRegistry() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		Assertions.assertThatThrownBy(() -> circuitBreakerRegistry.addConfiguration("default", CircuitBreakerConfig.custom().build()))
				.isExactlyInstanceOf(IllegalArgumentException.class);

	}

	@Test
	public void testCreateCircuitBreakerWithSharedConfiguration() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());
		final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("circuitBreaker",
				circuitBreakerRegistry.getConfiguration("testConfig").get());
		assertThat(circuitBreaker).isNotNull();
	}


	@Test
	public void testCreateCircuitBreakerWitMapConstructor() {
		Map<String, CircuitBreakerConfig> map = new HashMap<>();
		map.put("testBreaker", CircuitBreakerConfig.ofDefaults());
		CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry(map);
		circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());
		final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("circuitBreaker",
				circuitBreakerRegistry.getConfiguration("testConfig").get());
		assertThat(circuitBreaker).isNotNull();
	}

	@Test
	public void testCreateCircuitBreakerWithConfigName() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.custom().slidingWindowSize(5).build());
		final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("circuitBreaker",
				"testConfig");
		assertThat(circuitBreaker).isNotNull();
		assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(5);
	}

	@Test
	public void testCreateCircuitBreakerWithConfigNameNotFound() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		Assertions.assertThatThrownBy(() -> circuitBreakerRegistry.circuitBreaker("circuitBreaker",
				"testConfig")).isInstanceOf(ConfigurationNotFoundException.class);

	}


}