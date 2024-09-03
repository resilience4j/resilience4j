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


import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A {@link Metrics} implementation is backed by a sliding time window that aggregates only the
 * calls made in the last {@code N} seconds.
 * <p>
 * The sliding time window is implemented with a circular array of {@code N} partial aggregations
 * (buckets). If the time window size is 10 seconds, the circular array has always 10 partial
 * aggregations (buckets). Every bucket aggregates the outcome of all calls which happen in a
 * certain epoch second. (Partial aggregation) The head bucket of the circular array stores the call
 * outcomes of the current epoch second. The other partial aggregations store the call outcomes of
 * the previous {@code N-1} epoch seconds.
 * <p>
 * The sliding window does not store call outcomes (tuples) individually, but incrementally updates
 * partial aggregations (bucket) and a total aggregation. The total aggregation is updated
 * incrementally when a new call outcome is recorded. When the oldest bucket is evicted, the partial
 * total aggregation of that bucket is subtracted from the total aggregation. (Subtract-on-Evict)
 * <p>
 * The time to retrieve a Snapshot is constant 0(1), since the Snapshot is pre-aggregated and is
 * independent of the time window size. The space requirement (memory consumption) of this
 * implementation should be nearly constant O(n), since the call outcomes (tuples) are not stored
 * individually. Only {@code N} partial aggregations and 1 total aggregation are created.
 */
public class SlidingTimeWindowMetrics implements Metrics {

    final AtomicReferenceArray<PartialAggregation> partialAggregations;
    private final int timeWindowSizeInSeconds;
    private final TotalAggregation totalAggregation;
    private final Clock clock;
    final AtomicInteger headIndex;
    private final AtomicBoolean isMovingWindow = new AtomicBoolean(false);


    /**
     * Creates a new {@link SlidingTimeWindowMetrics} with the given clock and window of time.
     *
     * @param timeWindowSizeInSeconds the window time size in seconds
     * @param clock                   the {@link Clock} to use
     */
    public SlidingTimeWindowMetrics(int timeWindowSizeInSeconds, Clock clock) {
        this.clock = clock;
        this.timeWindowSizeInSeconds = timeWindowSizeInSeconds;
        this.partialAggregations = new AtomicReferenceArray<>(timeWindowSizeInSeconds);
        this.headIndex = new AtomicInteger();
        long epochSecond = clock.instant().getEpochSecond();
        for (int i = 0; i < timeWindowSizeInSeconds; i++) {
            partialAggregations.set(i, new PartialAggregation(epochSecond));
            epochSecond++;
        }
        this.totalAggregation = new TotalAggregation();
    }

    @Override
    public synchronized Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        totalAggregation.record(duration, durationUnit, outcome);
        moveWindowToCurrentEpochSecond().record(duration, durationUnit, outcome);
        return new SnapshotImpl(totalAggregation);
    }

    public synchronized Snapshot getSnapshot() {
        moveWindowToCurrentEpochSecond();
        return new SnapshotImpl(totalAggregation);
    }

    /**
     * Moves the end of the time window to the current epoch second. The latest bucket of the
     * circular array is used to calculate how many seconds the window must be moved. The difference
     * is calculated by subtracting the epoch second from the latest bucket from the current epoch
     * second. If the difference is greater than the time window size, the time window size is
     * used.
     *
     * @param latestPartialAggregation the latest partial aggregation of the circular array
     */
    private  PartialAggregation moveWindowToCurrentEpochSecond() {
        long currentEpochSecond = clock.instant().getEpochSecond();
        PartialAggregation latestPartialAggregation = getLatestPartialAggregation();
        long differenceInSeconds = currentEpochSecond - latestPartialAggregation.getEpochSecond();
        if (differenceInSeconds <= 0) {
            return latestPartialAggregation;
        }

        // Try to acquire the lock
        if (!isMovingWindow.compareAndSet(false, true)) {
            // If another thread is already moving the window, return the latest aggregation
            return latestPartialAggregation;
        }
        try {
            long secondsToMoveTheWindow = Math.min(differenceInSeconds, timeWindowSizeInSeconds);
            PartialAggregation currentPartialAggregation = null;
            do {
                secondsToMoveTheWindow--;
                moveHeadIndexByOne();
                currentPartialAggregation = getLatestPartialAggregation();
                totalAggregation.removeBucket(currentPartialAggregation);
                currentPartialAggregation.reset(currentEpochSecond - secondsToMoveTheWindow);
            } while (secondsToMoveTheWindow > 0);
            return currentPartialAggregation;
        } finally {
            // Release the lock
            isMovingWindow.set(false);
        }
    }

    /**
     * Returns the head partial aggregation of the circular array.
     *
     * @return the head partial aggregation of the circular array
     */
    private PartialAggregation getLatestPartialAggregation() {
        return partialAggregations.get(headIndex.get());
    }

    /**
     * Moves the headIndex to the next bucket.
     */
    void moveHeadIndexByOne() {
        headIndex.getAndUpdate(index -> (index + 1) % timeWindowSizeInSeconds);
    }
}