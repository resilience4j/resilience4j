package io.github.resilience4j.circuitbreaker.internal;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.function.Consumer;

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

}