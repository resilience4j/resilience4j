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

    @Test
    public void shouldReturnWitRecovery() {
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
        Try<String> result = Try.of(checkedSupplier)
                .recover((throwable) -> "Hello Recovery");

        //Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(circuitBreaker.isClosed()).isFalse();
        assertThat(result.get()).isEqualTo("Hello Recovery");

    }

    @Test
    public void testReadmeExample() {
        //Given
        CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry();

        // First parameter is maximum number of failures allowed
        // Second parameter is the wait interval [ms] and specifies how long the CircuitBreaker should stay OPEN
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig(1, 1000);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("uniqueName", circuitBreakerConfig);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is initially CLOSED
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is still CLOSED, because 1 failure is allowed
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // CircuitBreaker is OPEN, because maxFailures > 1

        //When
        // Wrap a standard Java8 Supplier with a CircuitBreaker
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> "Hello world", circuitBreaker);
        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isFailure()).isTrue(); // Call fails, because CircuitBreaker is OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // CircuitBreaker is OPEN, because maxFailures > 1
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class); // Exception was CircuitBreakerOpenException
    }


}
