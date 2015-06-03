package io.github.robwin.circuitbreaker;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.assertThat;


public class MonitorRegistryTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Before
    public void setUp(){
        circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry();
    }

    @Test
    public void shouldReturnTheCorrectName() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test
    public void shouldBeTheSameCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isSameAs(circuitBreaker2);
    }

    @Test
    public void shouldBeNotTheSameCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");

        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);
    }
}
