package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

interface AdaptiveBulkheadState {

    enum ThresholdResult {

        /**
         * Is below the error or slow calls rate.
         */
        BELOW_FAULT_RATE,

        /**
         * Is above the error or slow calls rate.
         */
        ABOVE_FAULT_RATE,

        /**
         * Is below minimum number of calls which are required (per sliding window period) before
         * the Adaptive Bulkhead can calculate the reliable error or slow calls rate.
         */
        UNRELIABLE
    }

    void onBelowThresholds();

    void onAboveThresholds();

    AdaptiveBulkhead.State getState();

    boolean isActive();
}
