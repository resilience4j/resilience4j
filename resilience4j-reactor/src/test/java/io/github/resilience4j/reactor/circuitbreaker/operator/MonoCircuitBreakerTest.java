package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import java.io.IOException;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class MonoCircuitBreakerTest extends CircuitBreakerAssertions {

  @Test
  public void shouldEmitEvent() {
    StepVerifier.create(
        Mono.just("Event")
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectNext("Event")
        .verifyComplete();

    assertSingleSuccessfulCall();
  }

  @Test
  public void shouldPropagateError() {
    StepVerifier.create(
        Mono.error(new IOException("BAM!"))
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectError(IOException.class)
        .verify();

    assertSingleFailedCall();
  }

  @Test
  public void shouldEmitErrorWithCircuitBreakerOpenException() {
    circuitBreaker.transitionToOpenState();
    StepVerifier.create(
        Mono.just("Event")
            .transform(CircuitBreakerOperator.of(circuitBreaker)))
        .expectError(CircuitBreakerOpenException.class)
        .verify();

    assertNoRegisteredCall();
  }
}