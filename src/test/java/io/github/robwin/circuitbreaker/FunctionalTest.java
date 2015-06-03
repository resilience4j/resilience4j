package io.github.robwin.circuitbreaker;

import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionalTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Before
    public void setUp(){
        circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry(new CircuitBreakerConfig(1, 1000));
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isFalse();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            throw new RuntimeException("BAM!");
        }, circuitBreaker);
        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(circuitBreaker.isClosed()).isFalse();
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            throw new RuntimeException("BAM!");
        }, circuitBreaker);

        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldReturnSuccess() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> "Hello world", circuitBreaker);
        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
    }
}
