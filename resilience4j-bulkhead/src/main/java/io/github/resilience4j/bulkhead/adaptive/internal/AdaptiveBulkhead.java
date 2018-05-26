package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.BulkheadAdaptationConfig;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;

import static java.lang.Math.*;

public class AdaptiveBulkhead {
    private static final double NANO_SCALE = 1_000_000_000d;
    private static final double MILLI_SCALE = 1_000_000d;
    private static final double LOW_LATENCY_MUL = 0.8d;
    private static final double CONCURRENCY_DROP_MUL = 0.85d;
    // TODO:bstorozhuk Add metrics and event publisher possibly
    // TODO:bstorozhuk configure LOW_LATENCY_MUL and CONCURRENCY_DROP_MUL
    // TODO:bstorozhuk try to unify measurement units for metrics
    // TODO:bstorozhuk replace adaptation window with cumulative moving average

    // initialization constants
    private final String name;
    private final BulkheadAdaptationConfig adaptationConfig;
    private final double initialMaxLatency;
    private final double desirableLatency;
    private final InternalMetrics metrics;
    private final Object windowsLock = new Object();

    private SemaphoreBulkhead bulkhead;

    // measurement window collections. They are !!!NOT THREAD SAFE!!!
    private MeasurementWindow adaptationWindow;
    // internal bulkhead that is thread safe;
    private MeasurementWindow reconfigurationWindow;

    // current settings and measurements that you can read concurrently to expose metrics
    private volatile BulkheadConfig currentConfig; // immutable object
    private volatile double currentMaxLatency;
    private volatile long currentAverageLatencyNanos;


    public AdaptiveBulkhead(String name, BulkheadAdaptationConfig config) {
        this.name = name;
        this.adaptationConfig = config;
        initialMaxLatency = config.getMaxAcceptableRequestLatency();
        desirableLatency = config.getDesirableOperationLatency();
        int initialConcurrency = (int) round(config.getDesirableAverageThroughput() * config.getDesirableOperationLatency());
        this.currentConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(initialConcurrency)
                .maxWaitTime(0)
                .build();
        currentMaxLatency = min(config.getDesirableOperationLatency() * 1.2d, config.getMaxAcceptableRequestLatency());

        int adaptationWindowSize = (int) ceil(config.getWindowForAdaptation().getSeconds() * config.getDesirableAverageThroughput());
        int reconfigurationWindowSize = (int) ceil(config.getWindowForReconfiguration().getSeconds() / config.getWindowForAdaptation().getSeconds());
        long initialLatencyInNanos = (long) (config.getDesirableOperationLatency() * NANO_SCALE);
        adaptationWindow = new MeasurementWindow(adaptationWindowSize, initialLatencyInNanos);
        reconfigurationWindow = new MeasurementWindow(reconfigurationWindowSize, initialLatencyInNanos);

        bulkhead = new SemaphoreBulkhead(name + "-internal", this.currentConfig);
        metrics = new InternalMetrics();
    }

    public boolean isCallPermitted() {
        return bulkhead.isCallPermitted();
    }

    public void onComplete(long durationInNanos) {
        bulkhead.onComplete();

        synchronized (windowsLock) {
            boolean endOfAdaptationWindow = adaptationWindow.measure(durationInNanos);
            if (endOfAdaptationWindow) {
                long averageLatencyNanos = adaptConcurrencyLevel();
                currentAverageLatencyNanos = averageLatencyNanos;
                boolean endOfReconfigurationWindow = reconfigurationWindow.measure(averageLatencyNanos);
                if (endOfReconfigurationWindow) {
                    adjustConfiguration(averageLatencyNanos);
                }
            }
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField") // should be called under {windowsLock}
    private void adjustConfiguration(long averageLatencyNanos) {
        long standardDeviationNanos = reconfigurationWindow.standardDeviation();
        // we can change latency only between desirableLatency * LOW_LATENCY_MUL and initialMaxLatency
        currentMaxLatency = min((double) (averageLatencyNanos + standardDeviationNanos) / NANO_SCALE, initialMaxLatency);
        currentMaxLatency = max(currentMaxLatency, desirableLatency * LOW_LATENCY_MUL);
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField") // should be called under {windowsLock}
    private long adaptConcurrencyLevel() {
        long averageLatencyNanos = adaptationWindow.average();
        double averageLatencySeconds = ((double) averageLatencyNanos) / NANO_SCALE;
        long waitTimeMillis = (long) (max(0d, desirableLatency - averageLatencySeconds) * MILLI_SCALE);
        if (averageLatencySeconds < currentMaxLatency) {
            currentConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(currentConfig.getMaxConcurrentCalls() + 1)
                    .maxWaitTime(waitTimeMillis)
                    .build();
        } else {
            currentConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(max((int) (currentConfig.getMaxConcurrentCalls() * CONCURRENCY_DROP_MUL), 1))
                    .maxWaitTime(waitTimeMillis)
                    .build();
        }
        bulkhead.changeConfig(currentConfig);
        return averageLatencyNanos;
    }

    public String getName() {
        return name;
    }

    public BulkheadAdaptationConfig getConfig() {
        return adaptationConfig;
    }

    public AdaptiveBulkheadMetrics getMetrics() {
        return metrics;
    }

    private final class InternalMetrics implements AdaptiveBulkheadMetrics {

        @Override
        public int getConcurrencyLimit() {
            return currentConfig.getMaxConcurrentCalls();
        }

        @Override
        public long getMaxPermissionWaitTimeMillis() {
            return currentConfig.getMaxWaitTime();
        }

        @Override
        public double getMaxLatencySeconds() {
            return currentMaxLatency;
        }

        @Override
        public long getAverageLatencyNanos() {
            return currentAverageLatencyNanos;
        }

        @Override
        public int getAvailableConcurrentCalls() {
            return bulkhead.getMetrics().getAvailableConcurrentCalls();
        }
    }
}
