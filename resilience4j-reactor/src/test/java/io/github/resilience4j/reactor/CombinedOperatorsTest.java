package io.github.resilience4j.reactor;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class CombinedOperatorsTest {

    private final RateLimiter rateLimiter = RateLimiter.of("test",
            RateLimiterConfig.custom().limitForPeriod(5).timeoutDuration(Duration.ZERO).limitRefreshPeriod(Duration.ofSeconds(10)).build());

    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("test",
            CircuitBreakerConfig.custom()
                    .waitDurationInOpenState(Duration.of(10, ChronoUnit.SECONDS))
                    .slidingWindowSize(4)
                    .permittedNumberOfCallsInHalfOpenState(4)
                    .build());

    private Bulkhead bulkhead = Bulkhead
            .of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());

    private final RetryConfig config = io.github.resilience4j.retry.RetryConfig.ofDefaults();
    private final Retry retry = Retry.of("testName", config);
    private final RetryOperator<String> retryOperator = RetryOperator.of(retry);

    @Test
    public void shouldEmitEvents() {
        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
        ).expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();
    }

    @Test
    public void shouldEmitEventsWithRetry() {
        StepVerifier.create(
                Flux.just("Event 1", "Event 2")
                        .compose(retryOperator)
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
        ).expectNext("Event 1")
                .expectNext("Event 2")
                .verifyComplete();
    }

    @Test
    public void shouldEmitEvent() {
        StepVerifier.create(
                Mono.just("Event 1")
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
        ).expectNext("Event 1")
                .verifyComplete();
    }

    @Test
    public void shouldEmitEventWithRetry() {
        StepVerifier.create(
                Mono.just("Event 1")
                        .compose(retryOperator)
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
        ).expectNext("Event 1")
                .verifyComplete();
    }

    @Test
    public void shouldPropagateError() {
        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
        ).expectError(IOException.class)
                .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldEmitErrorWithCircuitBreakerOpenExceptionEvenWhenErrorDuringSubscribe() {
        circuitBreaker.transitionToOpenState();
        StepVerifier.create(
                Flux.error(new IOException("BAM!"))
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
        ).expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldEmitErrorWithCircuitBreakerOpenExceptionEvenWhenErrorNotOnSubscribe() {
        circuitBreaker.transitionToOpenState();
        StepVerifier.create(
                Flux.error(new IOException("BAM!"), true)
                        .compose(CircuitBreakerOperator.of(circuitBreaker))
                        .compose(BulkheadOperator.of(bulkhead))
                        .compose(RateLimiterOperator.of(rateLimiter))
        ).expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));
    }
}
