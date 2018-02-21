package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

public class FluxRateLimiterTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();

        assertUsedPermits(2);
    }

    @Test
    public void shouldPropagateError() {
        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));

        assertSinglePermitUsed();
    }

    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        saturateRateLimiter();

        StepVerifier.create(
                Flux.just("Event")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }
}