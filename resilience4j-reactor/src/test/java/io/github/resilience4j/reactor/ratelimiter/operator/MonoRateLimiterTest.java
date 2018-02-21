package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

public class MonoRateLimiterTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        StepVerifier.create(
                Mono.just("Event")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event")
                .verifyComplete();

        assertSinglePermitUsed();
    }

    @Test
    public void shouldPropagateError() {
        StepVerifier.create(
                Mono.error(new IOException("BAM!"))
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
                Mono.just("Event")
                        .transform(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));

        assertNoPermitLeft();
    }
}