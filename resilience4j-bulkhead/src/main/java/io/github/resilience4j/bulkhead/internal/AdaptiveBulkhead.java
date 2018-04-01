package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.BulkheadAdaptationConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.*;

public class AdaptiveBulkhead {
    public static final AtomicInteger sleepMs = new AtomicInteger(500);
    public static final AtomicInteger testConcurrency = new AtomicInteger(35);
    // TODO:bstorozhuk Add metrics and event publisher possibly

    // initialization constants
    private final String name;
    private final double initialMaxLatency;
    private final double desirableLatency;
    private final Object windowsLock = new Object();
    // internal bulkhead that is thread safe;
    private SemaphoreBulkhead bulkhead;

    // measurement window collections. !!!NOT THREAD SAFE!!!
    private MeasurementWindow adaptationWindow;
    private MeasurementWindow reconfigurationWindow;

    // current settings and measurements that you can read concurrently to expose metrics
    private volatile BulkheadConfig config; // immutable object
    private volatile double currentMaxLatency;
    private volatile long currentAverageLatencyInNanos;

    public AdaptiveBulkhead(String name, BulkheadAdaptationConfig config) {
        this.name = name;
        initialMaxLatency = config.getMaxAcceptableRequestLatency();
        desirableLatency = config.getDesirableOperationLatency();
        int initialConcurrency = (int) round(config.getDesirableAverageThroughput() * config.getDesirableOperationLatency());
        this.config = BulkheadConfig.custom()
                .maxConcurrentCalls(initialConcurrency)
                .maxWaitTime(0)
                .build();
        currentMaxLatency = min(config.getDesirableOperationLatency() * 1.2d, config.getMaxAcceptableRequestLatency());

        int adaptationWindowSize = (int) ceil(config.getWindowForAdaptation().getSeconds() * config.getDesirableAverageThroughput());
        int reconfigurationWindowSize = (int) ceil(config.getWindowForReconfiguration().getSeconds() / config.getWindowForAdaptation().getSeconds());
        long initialLatencyInNanos = (long) (config.getDesirableOperationLatency() * 1_000_000_000d);
        adaptationWindow = new MeasurementWindow(adaptationWindowSize, initialLatencyInNanos);
        reconfigurationWindow = new MeasurementWindow(reconfigurationWindowSize, initialLatencyInNanos);

        bulkhead = new SemaphoreBulkhead(name + "-internal", this.config);
    }

    public boolean isCallPermitted() {
        return bulkhead.isCallPermitted();
    }

    public void onComplete(long durationInNanos) {
        bulkhead.onComplete();

        synchronized (windowsLock) {
            boolean endOfAdaptationWindow = adaptationWindow.measure(durationInNanos);
            if (endOfAdaptationWindow) {
                long averageLatencyInNanos = adaptConcurrencyLevel();
                currentAverageLatencyInNanos = averageLatencyInNanos;

//                System.out.println(config);
//                System.out.println("Current currentMaxLatency: " + currentMaxLatency);
//                System.out.println("Current currentAverageLatencyInNanos: " + currentAverageLatencyInNanos);
                System.out.printf("%d;%d;%d;%f;%f\n",
                        sleepMs.get(),
                        testConcurrency.get(),
                        config.getMaxConcurrentCalls(),
                        currentMaxLatency,
                        ((double) currentAverageLatencyInNanos) / 1_000_000_000d
                );
//                System.out.println("Adaptation window: " + adaptationWindow);
//                System.out.println("Reconfiguration window: " + reconfigurationWindow);

                boolean endOfReconfigurationWindow = reconfigurationWindow.measure(averageLatencyInNanos);
                if (endOfReconfigurationWindow) {
                    adjustConfiguration(averageLatencyInNanos);
                }
            }
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField") // should be called under {windowsLock}
    private void adjustConfiguration(long averageLatencyInNanos) {
        long standardDeviationInNanos = reconfigurationWindow.standardDeviation();
//        System.out.println("Current standardDeviationInNanos: " + standardDeviationInNanos);
        currentMaxLatency = min((double) (averageLatencyInNanos + standardDeviationInNanos) / 1_000_000_000d, initialMaxLatency);
        if (currentMaxLatency < desirableLatency * 0.8d) {
            currentMaxLatency = desirableLatency * 0.8d;
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField") // should be called under {windowsLock}
    private long adaptConcurrencyLevel() {
        long averageLatencyInNanos = adaptationWindow.average();
        double averageLatencyInSeconds = ((double) averageLatencyInNanos) / 1_000_000_000d;
        long waitTimeInMillis = (long) (max(0d, desirableLatency - averageLatencyInSeconds) * 1_000_000d);
        if (averageLatencyInSeconds < currentMaxLatency) {
            config = BulkheadConfig.custom()
                    .maxConcurrentCalls(config.getMaxConcurrentCalls() + 1)
                    .maxWaitTime(waitTimeInMillis)
                    .build();
        } else {
            config = BulkheadConfig.custom()
                    .maxConcurrentCalls(max((int) (config.getMaxConcurrentCalls() * 0.85d), 1))
                    .maxWaitTime(waitTimeInMillis)
                    .build();
        }
        bulkhead.changeConfig(config);
        return averageLatencyInNanos;
    }

    public String getName() {
        return name;
    }

    public BulkheadConfig getBulkheadConfig() {
        return config;
    }
}
