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

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.core.metrics.Metrics.Outcome;

class AdaptiveBulkheadMetrics implements AdaptiveBulkhead.Metrics {

    private final Metrics slidingWindowMetrics;
    private final float failureRateThreshold;
    private final float slowCallRateThreshold;
    private final long slowCallDurationThresholdInNanos;
    private final int minimumNumberOfCalls;
    private final Bulkhead.Metrics internalBulkheadMetrics;

    private AdaptiveBulkheadMetrics(int slidingWindowSize,
        AdaptiveBulkheadConfig.SlidingWindowType slidingWindowType,
        AdaptiveBulkheadConfig adaptiveBulkheadConfig,
        Bulkhead.Metrics internalBulkheadMetrics) {
        if (slidingWindowType == AdaptiveBulkheadConfig.SlidingWindowType.COUNT_BASED) {
            this.slidingWindowMetrics = new FixedSizeSlidingWindowMetrics(slidingWindowSize);
            this.minimumNumberOfCalls = Math
                .min(adaptiveBulkheadConfig.getMinimumNumberOfCalls(), slidingWindowSize);
        } else {
            this.slidingWindowMetrics = new SlidingTimeWindowMetrics(slidingWindowSize);
            this.minimumNumberOfCalls = adaptiveBulkheadConfig.getMinimumNumberOfCalls();
        }
        this.failureRateThreshold = adaptiveBulkheadConfig.getFailureRateThreshold();
        this.slowCallRateThreshold = adaptiveBulkheadConfig.getSlowCallRateThreshold();
        this.slowCallDurationThresholdInNanos = adaptiveBulkheadConfig
            .getSlowCallDurationThreshold().toNanos();
        this.internalBulkheadMetrics = internalBulkheadMetrics;
    }

    public AdaptiveBulkheadMetrics(AdaptiveBulkheadConfig adaptiveBulkheadConfig,
        Bulkhead.Metrics internalBulkheadMetrics) {
        this(adaptiveBulkheadConfig.getSlidingWindowSize(),
            adaptiveBulkheadConfig.getSlidingWindowType(),
            adaptiveBulkheadConfig,
            internalBulkheadMetrics);
    }

    /**
     * Records a successful call and checks if the thresholds are exceeded.
     *
     * @return the result of the check
     */
    public Result onSuccess(long duration, TimeUnit durationUnit) {
        return checkIfThresholdsExceeded(record(duration, durationUnit, true));
    }

    /**
     * Records a failed call and checks if the thresholds are exceeded.
     *
     * @return the result of the check
     */
    public Result onError(long duration, TimeUnit durationUnit) {
        return checkIfThresholdsExceeded(record(duration, durationUnit, false));
    }

    Snapshot record(long duration, TimeUnit durationUnit, boolean success) {
        boolean slow = durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos;
        return slidingWindowMetrics.record(duration, durationUnit, Outcome.of(slow, success));
    }

    /**
     * Checks if the failure rate is above the threshold or if the slow calls percentage is above
     * the threshold.
     *
     * @param snapshot a metrics snapshot
     * @return false, if the thresholds haven't been exceeded.
     */
    private Result checkIfThresholdsExceeded(Snapshot snapshot) {
        float failureRateInPercentage = getFailureRate(snapshot);
        if (failureRateInPercentage == -1) {
            return Result.BELOW_MINIMUM_CALLS_THRESHOLD;
        }
        if (failureRateInPercentage >= failureRateThreshold) {
            return Result.ABOVE_THRESHOLDS;
        }
        float slowCallsInPercentage = getSlowCallRate(snapshot);
        if (slowCallsInPercentage >= slowCallRateThreshold) {
            return Result.ABOVE_THRESHOLDS;
        }
        return Result.BELOW_THRESHOLDS;
    }

    private float getSlowCallRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < minimumNumberOfCalls) {
            return -1.0f;
        }
        return snapshot.getSlowCallRate();
    }

    private float getFailureRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < minimumNumberOfCalls) {
            return -1.0f;
        }
        return snapshot.getFailureRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFailureRate() {
        return getFailureRate(slidingWindowMetrics.getSnapshot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getSlowCallRate() {
        return getSlowCallRate(slidingWindowMetrics.getSnapshot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSuccessfulCalls() {
        return slidingWindowMetrics.getSnapshot().getNumberOfSuccessfulCalls();
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

    @Override
    public int getAvailableConcurrentCalls() {
        return internalBulkheadMetrics.getAvailableConcurrentCalls();
    }

    @Override
    public int getMaxAllowedConcurrentCalls() {
        return internalBulkheadMetrics.getMaxAllowedConcurrentCalls();
    }

    enum Result {
        /**
         * Is below the error or slow calls rate.
         */
        BELOW_THRESHOLDS,
        /**
         * Is above the error or slow calls rate.
         */
        ABOVE_THRESHOLDS,
        /**
         * Is below minimum number of calls which are required (per sliding window period) before
         * the Adaptive Bulkhead can calculate the error or slow calls rate.
         */
        BELOW_MINIMUM_CALLS_THRESHOLD
    }
}
