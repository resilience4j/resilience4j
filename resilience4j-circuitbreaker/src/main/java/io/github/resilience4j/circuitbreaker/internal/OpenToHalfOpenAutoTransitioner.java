package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.Lazy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enables the the circuit breaker to automatically transition from Open To Half Open state after a time has passed.
 * Used when automaticTransitionFromOpenToHalfOpenEnabled config property is set to true.
 */
public class OpenToHalfOpenAutoTransitioner {

    private OpenToHalfOpenAutoTransitioner() {
    }

    public static void scheduleAutoTransitionToHalfOpen(CircuitBreaker circuitBreaker, Instant retryAfterWaitDuration) {
        ScheduledExecutorService executorService = Lazy.of(Executors::newSingleThreadScheduledExecutor).get();

        Long millisUntilAutoTransitionToHalfOpen = Duration.between(ZonedDateTime.now(ZoneOffset.UTC),
                retryAfterWaitDuration.atZone(ZoneOffset.UTC)).toMillis();

        executorService.schedule(circuitBreaker::transitionToHalfOpenState,
                millisUntilAutoTransitionToHalfOpen, TimeUnit.MILLISECONDS);
    }

}