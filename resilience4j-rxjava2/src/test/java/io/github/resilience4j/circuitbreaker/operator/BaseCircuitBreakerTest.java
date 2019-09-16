package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.mockito.Mockito;

/**
 * Helper class to test and assert circuit breakers.
 */
abstract class BaseCircuitBreakerTest {

    CircuitBreaker circuitBreaker;
    HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        circuitBreaker = Mockito.mock(CircuitBreaker.class, Mockito.RETURNS_DEEP_STUBS);
        helloWorldService = Mockito.mock(HelloWorldService.class);
    }
}
