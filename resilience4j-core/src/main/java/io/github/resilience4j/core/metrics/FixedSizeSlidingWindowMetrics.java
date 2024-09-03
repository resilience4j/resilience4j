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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
    private final AtomicReferenceArray<Measurement> measurements;
    final AtomicInteger headIndex;

    /**
     * Creates a new {@link FixedSizeSlidingWindowMetrics} with the given window size.
     *
     * @param windowSize the window size
     */
    public FixedSizeSlidingWindowMetrics(int windowSize) {
        this.windowSize = windowSize;
        this.measurements = new AtomicReferenceArray<>(this.windowSize);
        this.headIndex = new AtomicInteger();
        for (int i = 0; i < this.windowSize; i++) {
            measurements.set(i, new Measurement());
        }
        this.totalAggregation = new TotalAggregation();
    }

    @Override
    public Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        totalAggregation.record(duration, durationUnit, outcome);
        moveWindowByOne(duration, durationUnit, outcome);
        return new SnapshotImpl(totalAggregation);
    }

    public Snapshot getSnapshot() {
        return new SnapshotImpl(totalAggregation);
    }

    private void moveWindowByOne(long duration, TimeUnit durationUnit, Outcome outcome) {
        int lastIndex = moveHeadIndexByOne();
        Measurement latestMeasurement = measurements.getAndSet(lastIndex, new Measurement(duration, durationUnit, outcome));
        totalAggregation.removeBucket(latestMeasurement);
    }

    /**
     * Moves the headIndex to the next bucket.
     */
    int moveHeadIndexByOne() {
        return headIndex.getAndUpdate(index -> (index + 1) % windowSize);
    }
}