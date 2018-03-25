package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.BulkheadAdaptationConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import static java.lang.Math.*;

public class AdaptiveBulkhead {
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

        int adaptationWindowSize = (int) round(ceil(config.getWindowForAdaptation().getSeconds() * config.getDesirableAverageThroughput()));
        int reconfigurationWindowSize = (int) round(ceil(config.getWindowForReconfiguration().getSeconds() * config.getDesirableAverageThroughput()));
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
        currentMaxLatency = min((double) (averageLatencyInNanos + standardDeviationInNanos) / 1_000_000_000d, initialMaxLatency);
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
                    .maxConcurrentCalls((int) (config.getMaxConcurrentCalls() * 0.75d))
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
