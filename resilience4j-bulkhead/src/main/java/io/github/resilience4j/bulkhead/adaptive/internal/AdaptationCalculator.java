/*
 *
 *  Copyright 2021: Tomasz Skowroński
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

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;

class AdaptationCalculator {

    private final AdaptiveBulkhead adaptiveBulkhead;

    AdaptationCalculator(AdaptiveBulkhead adaptiveBulkhead) {
        this.adaptiveBulkhead = adaptiveBulkhead;
    }

    /**
     * additive increase
     *
     * @return new concurrency limit to apply
     */
    int increment() {
        return fitToRange(
            getActualConcurrencyLimit() + getConfig().getIncreaseSummand());
    }

    /**
     * multiplicative increase
     *
     * @return new concurrency limit to apply
     */
    int increase() {
        return fitToRange(
            (int) (getActualConcurrencyLimit() * getConfig().getIncreaseMultiplier()));
    }

    /**
     * multiplicative decrease
     *
     * @return new concurrency limit to apply
     */
    int decrease() {
        return fitToRange(
            (int) (getActualConcurrencyLimit() * getConfig().getDecreaseMultiplier()));
    }

    private AdaptiveBulkheadConfig getConfig() {
        return adaptiveBulkhead.getBulkheadConfig();
    }

    private int getActualConcurrencyLimit() {
        return adaptiveBulkhead.getMetrics().getMaxAllowedConcurrentCalls();
    }

    private int fitToRange(int concurrencyLimitProposal) {
        return Math.min(getConfig().getMaxConcurrentCalls(),
            Math.max(getConfig().getMinConcurrentCalls(), concurrencyLimitProposal));
    }
}
