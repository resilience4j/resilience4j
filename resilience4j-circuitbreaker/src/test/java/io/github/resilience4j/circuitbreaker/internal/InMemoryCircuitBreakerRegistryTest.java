package io.github.resilience4j.circuitbreaker.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;


public class InMemoryCircuitBreakerRegistryTest {

	private Logger LOGGER;

	@Before
	public void setUp() {
		LOGGER = mock(Logger.class);
	}

	@Test
	public void testPostConsumerBeingCalled() {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		Consumer<CircuitBreaker> consumer1 = circuitBreaker -> LOGGER.info("invoking the post consumer1");
		Consumer<CircuitBreaker> consumer2 = circuitBreaker -> LOGGER.info("invoking the post consumer2");

		circuitBreakerRegistry.registerPostCreationConsumer(consumer1);

		circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");
		circuitBreakerRegistry.circuitBreaker("testCircuitBreaker2", CircuitBreakerConfig.ofDefaults());
		circuitBreakerRegistry.circuitBreaker("testCircuitBreaker3", CircuitBreakerConfig::ofDefaults);

		then(LOGGER).should(times(3)).info("invoking the post consumer1");

		circuitBreakerRegistry.registerPostCreationConsumer(consumer2);
		circuitBreakerRegistry.unregisterPostCreationConsumer(consumer1);
		circuitBreakerRegistry.circuitBreaker("testCircuitBreaker4");
		then(LOGGER).should(times(1)).info("invoking the post consumer2");
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
				.isExactlyInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("you can not use 'default'");

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


}