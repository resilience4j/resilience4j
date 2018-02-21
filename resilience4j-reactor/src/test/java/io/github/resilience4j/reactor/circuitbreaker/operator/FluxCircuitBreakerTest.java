package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import java.io.IOException;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class FluxCircuitBreakerTest extends CircuitBreakerAssertions {

  @Test
  public void shouldEmitEvent() {
    StepVerifier.create(
        Flux.just("Event 1", "Event 2")
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectNext("Event 1")
        .expectNext("Event 2")
        .verifyComplete();

    assertSingleSuccessfulCall();
  }

  @Test
  public void shouldPropagateError() {
    StepVerifier.create(
        Flux.error(new IOException("BAM!"))
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectError(IOException.class)
        .verify();

    assertSingleFailedCall();
  }

  @Test
  public void shouldEmitErrorWithCircuitBreakerOpenException() {
    circuitBreaker.transitionToOpenState();
    StepVerifier.create(
        Flux.just("Event 1", "Event 2")
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectError(CircuitBreakerOpenException.class)
        .verify();

    assertNoRegisteredCall();
  }
}