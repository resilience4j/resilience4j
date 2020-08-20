package io.github.resilience4j.circuitbreaker.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SchedulerFactory {

    private static class SchedulerFactoryInstance {
        private static final SchedulerFactory lazyInstance =  new SchedulerFactory();
        private static final ScheduledExecutorService lazyScheduler = Executors.newSingleThreadScheduledExecutor(threadTask -> {
                Thread thread = new Thread(threadTask, "CircuitBreakerAutoTransitionThread");
                thread.setDaemon(true);
                return thread;
            });
    }

    private SchedulerFactory() {
    }

    public static SchedulerFactory getInstance() {
        return SchedulerFactoryInstance.lazyInstance;
    }

    public ScheduledExecutorService getScheduler() {
        return SchedulerFactoryInstance.lazyScheduler;
    }
}
