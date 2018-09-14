package io.github.resilience4j.circuitbreaker.internal;

import io.vavr.Lazy;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules tasks to be completed after a duration. E.g. to automatically transition from open to half open state
 * when automaticTransitionFromOpenToHalfOpenEnabled config property is set to true.
 */
public class AutoTransitioner {

    private static final Lazy<ScheduledExecutorService> executorService = Lazy.of(
            Executors::newSingleThreadScheduledExecutor);

    private AutoTransitioner() {
    }

    public static void scheduleAutoTransition(Runnable transition, Duration waitDurationInOpenState) {
        executorService.get().schedule(
                transition,
                waitDurationInOpenState.toMillis(),
                TimeUnit.MILLISECONDS);
    }
}