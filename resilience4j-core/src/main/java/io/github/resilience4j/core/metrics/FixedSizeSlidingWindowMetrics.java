/*
 *
 *  Copyright 2019 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.core.metrics;


import java.util.concurrent.TimeUnit;

/**
 * A {@link Metrics} implementation is backed by a sliding window that aggregates only the last
 * {@code N} calls.
 * <p>
 * The sliding window is implemented with a circular array of {@code N} measurements. If the time
 * window size is 10, the circular array has always 10 measurements.
 * <p>
 * The sliding window incrementally updates a total aggregation. The total aggregation is updated
 * incrementally when a new call outcome is recorded. When the oldest measurement is evicted, the
 * measurement is subtracted from the total aggregation. (Subtract-on-Evict)
 * <p>
 * The time to retrieve a Snapshot is constant 0(1), since the Snapshot is pre-aggregated and is
 * independent of the window size. The space requirement (memory consumption) of this implementation
 * should be O(n).
 */
public class FixedSizeSlidingWindowMetrics implements Metrics {

    private final int windowSize;
    private final TotalAggregation totalAggregation;
    private final Measurement[] measurements;
    private int headIndex;

    /**
     * Creates a new {@link FixedSizeSlidingWindowMetrics} with the given window size.
     *
     * @param windowSize the window size
     */
    public FixedSizeSlidingWindowMetrics(int windowSize) {
        this.windowSize = windowSize;
        this.measurements = new Measurement[this.windowSize];
        this.headIndex = 0;
        for (int i = 0; i < this.windowSize; i++) {
            measurements[i] = new Measurement();
        }
        this.totalAggregation = new TotalAggregation();
    }

    @Override
    public synchronized Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        totalAggregation.record(duration, durationUnit, outcome);
        moveWindowByOne().record(duration, durationUnit, outcome);
        return new SnapshotImpl(totalAggregation);
    }

    @Override
    public void resetRecords() {
        headIndex = 0;
        totalAggregation.reset();
        for (Measurement measurement : measurements) {
            measurement.reset();
        }
    }

    public synchronized Snapshot getSnapshot() {
        return new SnapshotImpl(totalAggregation);
    }

    private Measurement moveWindowByOne() {
        moveHeadIndexByOne();
        Measurement latestMeasurement = getLatestMeasurement();
        totalAggregation.removeBucket(latestMeasurement);
        latestMeasurement.reset();
        return latestMeasurement;
    }

    /**
     * Returns the head partial aggregation of the circular array.
     *
     * @return the head partial aggregation of the circular array
     */
    private Measurement getLatestMeasurement() {
        return measurements[headIndex];
    }

    /**
     * Moves the headIndex to the next bucket.
     */
    void moveHeadIndexByOne() {
        this.headIndex = (headIndex + 1) % windowSize;
    }

    int getHeadIndex() {
        return headIndex;
    }
}