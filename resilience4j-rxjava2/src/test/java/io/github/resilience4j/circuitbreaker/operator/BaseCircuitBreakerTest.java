package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Helper class to test and assert circuit breakers.
 */
abstract class BaseCircuitBreakerTest {

    CircuitBreaker circuitBreaker;
    HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);
        helloWorldService = mock(HelloWorldService.class);
    }
}
