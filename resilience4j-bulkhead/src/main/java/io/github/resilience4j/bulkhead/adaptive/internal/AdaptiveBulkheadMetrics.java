/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.bulkhead.adaptive.internal;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.core.metrics.Metrics.Outcome;

class AdaptiveBulkheadMetrics implements AdaptiveBulkhead.Metrics {

    private final Metrics slidingWindowMetrics;
    private final float failureRateThreshold;
    private final float slowCallRateThreshold;
    private final long slowCallDurationThresholdInNanos;
    private final int minimumNumberOfCalls;
    private final Bulkhead.Metrics innerMetrics;

    private AdaptiveBulkheadMetrics(int slidingWindowSize,
                                    AdaptiveBulkheadConfig.SlidingWindowType slidingWindowType,
                                    AdaptiveBulkheadConfig adaptiveBulkheadConfig,
                                    Bulkhead.Metrics innerMetrics,
                                    Clock clock) {
        if (slidingWindowType == AdaptiveBulkheadConfig.SlidingWindowType.COUNT_BASED) {
            this.slidingWindowMetrics = new FixedSizeSlidingWindowMetrics(slidingWindowSize);
            this.minimumNumberOfCalls = Math
                .min(adaptiveBulkheadConfig.getMinimumNumberOfCalls(), slidingWindowSize);
        } else {
            this.slidingWindowMetrics = new SlidingTimeWindowMetrics(slidingWindowSize, clock);
            this.minimumNumberOfCalls = adaptiveBulkheadConfig.getMinimumNumberOfCalls();
        }
        this.failureRateThreshold = adaptiveBulkheadConfig.getFailureRateThreshold();
        this.slowCallRateThreshold = adaptiveBulkheadConfig.getSlowCallRateThreshold();
        this.slowCallDurationThresholdInNanos = adaptiveBulkheadConfig
            .getSlowCallDurationThreshold().toNanos();
        this.innerMetrics = innerMetrics;
    }

    public AdaptiveBulkheadMetrics(AdaptiveBulkheadConfig adaptiveBulkheadConfig,
                                   Bulkhead.Metrics innerMetrics,
                                   Clock clock) {
        this(adaptiveBulkheadConfig.getSlidingWindowSize(),
            adaptiveBulkheadConfig.getSlidingWindowType(),
            adaptiveBulkheadConfig,
            innerMetrics,
            clock);
    }

    /**
     * Records a successful call
     *
     * @return the ThresholdResult
     */
    public AdaptiveBulkheadState.ThresholdResult onSuccess(long nanoseconds) {
        return recordCall(nanoseconds, true);
    }

    /**
     * Records a failed call
     *
     * @return the ThresholdResult
     */
    public AdaptiveBulkheadState.ThresholdResult onError(long nanoseconds) {
        return recordCall(nanoseconds, false);
    }

    private AdaptiveBulkheadState.ThresholdResult recordCall(long nanoseconds, boolean success) {
        Outcome outcome = Outcome.of(nanoseconds > slowCallDurationThresholdInNanos, success);
        Snapshot snapshot = slidingWindowMetrics.record(nanoseconds, TimeUnit.NANOSECONDS, outcome);
        return thresholdsExcess(snapshot);
    }

    /**
     * Checks if the failure rate is above the threshold or if the slow calls' percentage is above the threshold.
     *
     * @param snapshot a metrics snapshot
     * @return thresholds haven been exceeded
     */
    private AdaptiveBulkheadState.ThresholdResult thresholdsExcess(Snapshot snapshot) {
        if (isBelowMinimumNumberOfCalls(snapshot)) {
            return AdaptiveBulkheadState.ThresholdResult.UNRELIABLE;
        } else if (isAboveFaultRate(snapshot)) {
            return AdaptiveBulkheadState.ThresholdResult.ABOVE_FAULT_RATE;
        } else {
            return AdaptiveBulkheadState.ThresholdResult.BELOW_FAULT_RATE;
        }
    }

    private boolean isBelowMinimumNumberOfCalls(Snapshot snapshot) {
        return snapshot.getTotalNumberOfCalls() == 0
            || snapshot.getTotalNumberOfCalls() < minimumNumberOfCalls;
    }

    private boolean isAboveFaultRate(Snapshot snapshot) {
        return snapshot.getFailureRate() >= failureRateThreshold
            || snapshot.getSlowCallRate() >= slowCallRateThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFailureRate() {
        Snapshot snapshot = slidingWindowMetrics.getSnapshot();
        return isBelowMinimumNumberOfCalls(snapshot) ? -1 : snapshot.getFailureRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getSlowCallRate() {
        Snapshot snapshot = slidingWindowMetrics.getSnapshot();
        return isBelowMinimumNumberOfCalls(snapshot) ? -1 : snapshot.getSlowCallRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSuccessfulCalls() {
        return slidingWindowMetrics.getSnapshot().getNumberOfSuccessfulCalls();
    }

    @Override
    public void resetRecords() {
        slidingWindowMetrics.resetRecords();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfBufferedCalls() {
        return slidingWindowMetrics.getSnapshot().getTotalNumberOfCalls();
    }

    @Override
    public int getNumberOfFailedCalls() {
        return slidingWindowMetrics.getSnapshot().getNumberOfFailedCalls();
    }

    @Override
    public int getNumberOfSlowCalls() {
        return slidingWindowMetrics.getSnapshot().getTotalNumberOfSlowCalls();
    }

    @Override
    public int getNumberOfSlowSuccessfulCalls() {
        return slidingWindowMetrics.getSnapshot().getNumberOfSlowSuccessfulCalls();
    }

    @Override
    public int getNumberOfSlowFailedCalls() {
        return slidingWindowMetrics.getSnapshot().getNumberOfSlowFailedCalls();
    }

    Snapshot getSnapshot() {
        return slidingWindowMetrics.getSnapshot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAvailableConcurrentCalls() {
        return innerMetrics.getAvailableConcurrentCalls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxAllowedConcurrentCalls() {
        return innerMetrics.getMaxAllowedConcurrentCalls();
    }

}
