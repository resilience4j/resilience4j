/*
 *
 *  Copyright 2024 Florentin Simion and Rares Vlasceanu
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * A measurement implementation used in sliding windows to track the total duration and the number of calls in the
 * window, along with the duration and number of calls of the current entry/bucket.
 *
 * <p>This implementation has the advantage of being cache friendly, benefiting from cache locality when
 * counting/discarding the tracked metrics.
 *
 * <p>Besides this, metrics can also be quickly cloned, which is important for the lock-free algorithms which
 * are operating with immutable objects.
 */
public class PackedAggregation implements CumulativeMeasurement {
    private long[] durations = new long[2];
    private int[] counts = new int[8];

    private static final int TOTAL_DURATION_INDEX = 0;
    private static final int DURATION_INDEX = 1;

    private static final int TOTAL_SLOW_CALLS_INDEX = 0;
    private static final int TOTAL_FAILED_SLOW_CALLS_INDEX = 1;
    private static final int TOTAL_FAILED_CALLS_INDEX = 2;
    private static final int TOTAL_CALLS_INDEX = 3;

    private static final int SLOW_CALLS_INDEX = 4;
    private static final int FAILED_SLOW_CALLS_INDEX = 5;
    private static final int FAILED_CALLS_INDEX = 6;
    private static final int CALLS_INDEX = 7;

    public PackedAggregation() {
    }

    public PackedAggregation(long[] durations, int[] counts) {
        this.durations = durations;
        this.counts = counts;
    }

    PackedAggregation copy() {
        return new PackedAggregation(durations.clone(), counts.clone());
    }

    void discard(PackedAggregation discarded) {
        durations[TOTAL_DURATION_INDEX] -= discarded.durations[DURATION_INDEX];
        durations[DURATION_INDEX] = 0;

        counts[TOTAL_SLOW_CALLS_INDEX] -= discarded.counts[SLOW_CALLS_INDEX];
        counts[TOTAL_FAILED_SLOW_CALLS_INDEX] -= discarded.counts[FAILED_SLOW_CALLS_INDEX];
        counts[TOTAL_FAILED_CALLS_INDEX] -= discarded.counts[FAILED_CALLS_INDEX];
        counts[TOTAL_CALLS_INDEX] -= discarded.counts[CALLS_INDEX];

        counts[SLOW_CALLS_INDEX] = 0;
        counts[FAILED_SLOW_CALLS_INDEX] = 0;
        counts[FAILED_CALLS_INDEX] = 0;
        counts[CALLS_INDEX] = 0;
    }

    @Override
    public void record(long duration, TimeUnit durationUnit, Metrics.Outcome outcome) {
        long durationInMillis = durationUnit.toMillis(duration);

        durations[TOTAL_DURATION_INDEX] += durationInMillis;
        durations[DURATION_INDEX] += durationInMillis;

        counts[TOTAL_CALLS_INDEX]++;
        counts[CALLS_INDEX]++;

        switch (outcome) {
            case SLOW_SUCCESS:
                counts[TOTAL_SLOW_CALLS_INDEX]++;
                counts[SLOW_CALLS_INDEX]++;
                break;

            case SLOW_ERROR:
                counts[TOTAL_SLOW_CALLS_INDEX]++;
                counts[SLOW_CALLS_INDEX]++;

                counts[TOTAL_FAILED_SLOW_CALLS_INDEX]++;
                counts[FAILED_SLOW_CALLS_INDEX]++;

                counts[TOTAL_FAILED_CALLS_INDEX]++;
                counts[FAILED_CALLS_INDEX]++;
                break;

            case ERROR:
                counts[TOTAL_FAILED_CALLS_INDEX]++;
                counts[FAILED_CALLS_INDEX]++;
                break;

            default:
                break;
        }
    }

    @Override
    public long getTotalDurationInMillis() {
        return durations[TOTAL_DURATION_INDEX];
    }

    @Override
    public int getNumberOfSlowCalls() {
        return counts[TOTAL_SLOW_CALLS_INDEX];
    }

    @Override
    public int getNumberOfSlowFailedCalls() {
        return counts[TOTAL_FAILED_SLOW_CALLS_INDEX];
    }

    @Override
    public int getNumberOfFailedCalls() {
        return counts[TOTAL_FAILED_CALLS_INDEX];
    }

    @Override
    public int getNumberOfCalls() {
        return counts[TOTAL_CALLS_INDEX];
    }

    @Override
    public String toString() {
        return "PackedAggregation{" +
            "durations=" + Arrays.toString(durations) +
            ", counts=" + Arrays.toString(counts) +
            '}';
    }
}
