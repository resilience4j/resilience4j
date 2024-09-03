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

import java.time.Duration;

public class SnapshotImpl implements Snapshot {

    private final long totalDurationInMillis;
    private final int totalNumberOfSlowCalls;
    private final int totalNumberOfSlowFailedCalls;
    private final int totalNumberOfFailedCalls;
    private final int totalNumberOfCalls;

    SnapshotImpl(TotalAggregation totalAggregation) {
        this.totalDurationInMillis = totalAggregation.totalDurationInMillis.sum();
        this.totalNumberOfSlowCalls = totalAggregation.numberOfSlowCalls.intValue();
        this.totalNumberOfSlowFailedCalls = totalAggregation.numberOfSlowFailedCalls.intValue();
        this.totalNumberOfFailedCalls = totalAggregation.numberOfFailedCalls.intValue();
        this.totalNumberOfCalls = totalAggregation.numberOfCalls.intValue();

    }

    @Override
    public Duration getTotalDuration() {
        return Duration.ofMillis(totalDurationInMillis);
    }

    @Override
    public int getTotalNumberOfSlowCalls() {
        return totalNumberOfSlowCalls;
    }

    @Override
    public int getNumberOfSlowSuccessfulCalls() {
        return totalNumberOfSlowCalls - totalNumberOfSlowFailedCalls;
    }

    @Override
    public int getNumberOfSlowFailedCalls() {
        return totalNumberOfSlowFailedCalls;
    }

    @Override
    public float getSlowCallRate() {
        if (totalNumberOfCalls == 0) {
            return 0;
        }
        return totalNumberOfSlowCalls * 100.0f / totalNumberOfCalls;
    }

    @Override
    public int getNumberOfSuccessfulCalls() {
        return totalNumberOfCalls - totalNumberOfFailedCalls;
    }

    @Override
    public int getNumberOfFailedCalls() {
        return totalNumberOfFailedCalls;
    }

    @Override
    public int getTotalNumberOfCalls() {
        return totalNumberOfCalls;
    }

    @Override
    public float getFailureRate() {
        if (totalNumberOfCalls == 0) {
            return 0;
        }
        return totalNumberOfFailedCalls * 100.0f / totalNumberOfCalls;
    }

    @Override
    public Duration getAverageDuration() {
        if (totalNumberOfCalls == 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(totalDurationInMillis / totalNumberOfCalls);
    }
}
