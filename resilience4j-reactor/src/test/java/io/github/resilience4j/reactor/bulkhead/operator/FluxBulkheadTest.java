package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.io.IOException;
import java.time.Duration;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class FluxBulkheadTest {

  private Bulkhead bulkhead = Bulkhead
      .of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitTime(0).build());

  @Test
  public void shouldEmitEvent() {
    StepVerifier.create(
        Flux.just("Event 1", "Event 2")
            .transform(io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator.of(bulkhead)))
        .expectNext("Event 1")
        .expectNext("Event 2")
        .verifyComplete();

    assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
  }

  @Test
  public void shouldPropagateError() {
    StepVerifier.create(
        Flux.error(new IOException("BAM!"))
            .transform(io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator.of(bulkhead)))
        .expectSubscription()
        .expectError(IOException.class)
        .verify(Duration.ofSeconds(1));

    assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
  }

  @Test
  public void shouldEmitErrorWithBulkheadFullException() {
    bulkhead.isCallPermitted();

    StepVerifier.create(
        Flux.just("Event")
            .transform(BulkheadOperator.of(bulkhead)))
        .expectSubscription()
        .expectError(BulkheadFullException.class)
        .verify(Duration.ofSeconds(1));

    assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);

  }
}