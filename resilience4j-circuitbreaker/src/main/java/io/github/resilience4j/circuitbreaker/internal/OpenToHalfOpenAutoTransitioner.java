package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enables the the circuit breaker to automatically transition from Open To Half Open state after a time has passed.
 * Used when enableAutomaticTransitionFromOpenToHalfOpen config property is set to true.
 */
public class OpenToHalfOpenAutoTransitioner {

    private OpenToHalfOpenAutoTransitioner() {
    }

    public static void scheduleAutoTransitionToHalfOpen(CircuitBreaker circuitBreaker) {
        if (circuitBreaker.getRetryAfterWaitDuration().isEmpty()
                || !circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
            return;
        }

        ScheduledExecutorService executorService = LazyScheduledExecutorService.getExecutorService();

        Long millisUntilAutoTransitionToHalfOpen = Duration.between(ZonedDateTime.now(ZoneOffset.UTC),
                circuitBreaker.getRetryAfterWaitDuration().get().atZone(ZoneOffset.UTC)).toMillis();

        executorService.schedule(circuitBreaker::transitionToHalfOpenState,
                millisUntilAutoTransitionToHalfOpen, TimeUnit.MILLISECONDS);
    }

    private static class LazyScheduledExecutorService {

        private static final Object mutex = new Object();
        private static volatile ScheduledExecutorService executorService = null;

        private static ScheduledExecutorService getExecutorService() {
            ScheduledExecutorService result = executorService;
            if (result != null) {
                return result;
            }

            synchronized (mutex) {
                result = executorService;
                if (result == null) {
                    executorService = result = Executors.newSingleThreadScheduledExecutor();
                }
            }
            return result;
        }
    }
}