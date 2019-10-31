package io.github.resilience4j.circuitbreaker.internal;

import io.vavr.Lazy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SchedulerFactory {

    private static Lazy<SchedulerFactory> lazyInstance = Lazy.of(SchedulerFactory::new);
    private Lazy<ScheduledExecutorService> lazyScheduler = Lazy
        .of(() -> Executors.newSingleThreadScheduledExecutor(threadTask -> {
            Thread thread = new Thread(threadTask, "CircuitBreakerAutoTransitionThread");
            thread.setDaemon(true);
            return thread;
        }));

    private SchedulerFactory() {
    }

    public static SchedulerFactory getInstance() {
        return lazyInstance.get();
    }

    public ScheduledExecutorService getScheduler() {
        return lazyScheduler.get();
    }
}
