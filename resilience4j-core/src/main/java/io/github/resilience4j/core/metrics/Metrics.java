package io.github.resilience4j.core.metrics;

import java.util.concurrent.TimeUnit;

public interface Metrics {

    /**
     * Records a call.
     *
     * @param duration the duration of the call
     * @param durationUnit the time unit of the duration
     * @param outcome the outcome of the call
     */
    Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome);

    /**
     * Returns a snapshot.
     *
     * @return a snapshot
     */
    Snapshot getSnapshot();

    enum Outcome
    {
        SUCCESS, ERROR, SLOW_SUCCESS
    }

}